package com.bonitasoft.connectors.dynamics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Slf4j
public class DynamicsClient {

    private final DynamicsConfiguration configuration;
    private final RetryPolicy retryPolicy;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String accessToken;

    public DynamicsClient(DynamicsConfiguration configuration) throws DynamicsException {
        this.configuration = configuration;
        this.retryPolicy = new RetryPolicy(configuration.getMaxRetries());
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(configuration.getConnectTimeout()))
                .build();
        authenticate();
        log.debug("DynamicsClient initialized for {}", configuration.getOrganizationUrl());
    }

    private void authenticate() throws DynamicsException {
        try {
            String tokenUrl = String.format(
                    "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
                    configuration.getTenantId());

            String body = String.format(
                    "grant_type=client_credentials&client_id=%s&client_secret=%s&scope=%s",
                    URLEncoder.encode(configuration.getClientId(), StandardCharsets.UTF_8),
                    URLEncoder.encode(configuration.getClientSecret(), StandardCharsets.UTF_8),
                    URLEncoder.encode(normalizeOrgUrl() + "/.default", StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMillis(configuration.getConnectTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new DynamicsException(
                        "Authentication failed (HTTP " + response.statusCode() + "): " + response.body(),
                        response.statusCode(), false);
            }

            JsonNode json = objectMapper.readTree(response.body());
            this.accessToken = json.get("access_token").asText();
            log.debug("OAuth2 token acquired successfully");
        } catch (DynamicsException e) {
            throw e;
        } catch (Exception e) {
            throw new DynamicsException("Authentication failed: " + e.getMessage(), e);
        }
    }

    private void refreshToken() throws DynamicsException {
        log.debug("Refreshing OAuth2 token...");
        authenticate();
    }

    private String normalizeOrgUrl() {
        String url = configuration.getOrganizationUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String apiBase() {
        return normalizeOrgUrl() + "/api/data/" + configuration.getApiVersion();
    }

    private HttpRequest.Builder authenticatedRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("OData-MaxVersion", "4.0")
                .header("OData-Version", "4.0")
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(configuration.getReadTimeout()));
    }

    private HttpResponse<String> executeWithAuth(HttpRequest request) throws DynamicsException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                log.info("Received 401, refreshing token and retrying...");
                refreshToken();
                // Rebuild request with new token
                HttpRequest retryRequest = authenticatedRequest(request.uri().toString())
                        .method(request.method(),
                                request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
                        .build();
                // Copy headers from original except Authorization
                response = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 401) {
                    throw new DynamicsException("Authentication failed after token refresh", 401, false);
                }
            }

            return response;
        } catch (DynamicsException e) {
            throw e;
        } catch (IOException e) {
            throw new DynamicsException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DynamicsException("Request interrupted", e);
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) throws DynamicsException {
        int status = response.statusCode();
        String errorDetail = extractErrorMessage(response.body());
        boolean retryable = RetryPolicy.isRetryableStatusCode(status);

        if (status == 403) {
            throw new DynamicsException("Forbidden: insufficient privileges - " + errorDetail, status, false);
        }
        if (status == 409) {
            throw new DynamicsException("Conflict: " + errorDetail, status, false);
        }
        if (status == 412) {
            throw new DynamicsException("ETag mismatch (precondition failed): " + errorDetail, status, false);
        }

        throw new DynamicsException("HTTP " + status + ": " + errorDetail, status, retryable);
    }

    private String extractErrorMessage(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            JsonNode error = json.get("error");
            if (error != null && error.has("message")) {
                return error.get("message").asText();
            }
        } catch (Exception ignored) {
            // Not JSON or no error field
        }
        return body != null && body.length() > 500 ? body.substring(0, 500) : (body != null ? body : "");
    }

    public CreateEntityResult createEntity(DynamicsConfiguration config) throws DynamicsException {
        return retryPolicy.execute(() -> {
            String url = apiBase() + "/" + config.getEntityType();

            HttpRequest.Builder requestBuilder = authenticatedRequest(url)
                    .header("Content-Type", "application/json");

            if (Boolean.TRUE.equals(config.getReturnRepresentation())) {
                requestBuilder.header("Prefer", "return=representation");
            }

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(config.getEntityData()))
                    .build();

            HttpResponse<String> response = executeWithAuth(request);

            if (response.statusCode() != 200 && response.statusCode() != 201 && response.statusCode() != 204) {
                handleErrorResponse(response);
            }

            String entityId = null;
            String entityUrl = null;

            // Extract entity ID from OData-EntityId header
            var entityIdHeader = response.headers().firstValue("OData-EntityId").orElse(null);
            if (entityIdHeader != null) {
                entityUrl = entityIdHeader;
                // Extract GUID from URL like https://org.crm.dynamics.com/api/data/v9.2/accounts(guid)
                int openParen = entityIdHeader.lastIndexOf('(');
                int closeParen = entityIdHeader.lastIndexOf(')');
                if (openParen >= 0 && closeParen > openParen) {
                    entityId = entityIdHeader.substring(openParen + 1, closeParen);
                }
            }

            String responseBody = response.body();
            if (entityId == null && responseBody != null && !responseBody.isEmpty()) {
                try {
                    JsonNode json = objectMapper.readTree(responseBody);
                    var fieldNames = json.fieldNames();
                    while (fieldNames.hasNext()) {
                        String field = fieldNames.next();
                        if (field.endsWith("id") && json.get(field).isTextual()) {
                            entityId = json.get(field).asText();
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    // Best effort
                }
            }

            return new CreateEntityResult(entityId, entityUrl, responseBody);
        });
    }

    public UpdateEntityResult updateEntity(DynamicsConfiguration config) throws DynamicsException {
        return retryPolicy.execute(() -> {
            String url = apiBase() + "/" + config.getEntityType() + "(" + config.getEntityId() + ")";

            HttpRequest.Builder requestBuilder = authenticatedRequest(url)
                    .header("Content-Type", "application/json");

            if (Boolean.TRUE.equals(config.getReturnRepresentation())) {
                requestBuilder.header("Prefer", "return=representation");
            }
            if (config.getIfMatch() != null && !config.getIfMatch().isBlank()) {
                requestBuilder.header("If-Match", config.getIfMatch());
            }

            HttpRequest request = requestBuilder
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(config.getEntityData()))
                    .build();

            HttpResponse<String> response = executeWithAuth(request);

            if (response.statusCode() != 200 && response.statusCode() != 204) {
                handleErrorResponse(response);
            }

            return new UpdateEntityResult(config.getEntityId(), response.body());
        });
    }

    public GetEntityResult getEntity(DynamicsConfiguration config) throws DynamicsException {
        return retryPolicy.execute(() -> {
            StringBuilder url = new StringBuilder(apiBase() + "/" + config.getEntityType()
                    + "(" + config.getEntityId() + ")");

            String separator = "?";
            if (config.getSelectFields() != null && !config.getSelectFields().isBlank()) {
                url.append(separator).append("$select=").append(
                        URLEncoder.encode(config.getSelectFields(), StandardCharsets.UTF_8));
                separator = "&";
            }
            if (config.getExpandRelations() != null && !config.getExpandRelations().isBlank()) {
                url.append(separator).append("$expand=").append(
                        URLEncoder.encode(config.getExpandRelations(), StandardCharsets.UTF_8));
            }

            HttpRequest request = authenticatedRequest(url.toString())
                    .GET()
                    .build();

            HttpResponse<String> response = executeWithAuth(request);

            if (response.statusCode() == 404) {
                throw new DynamicsException("Entity not found: " + config.getEntityId(), 404, false);
            }
            if (response.statusCode() != 200) {
                handleErrorResponse(response);
            }

            String etag = response.headers().firstValue("ETag").orElse(null);

            return new GetEntityResult(config.getEntityId(), response.body(), etag);
        });
    }

    public QueryEntitiesResult queryEntities(DynamicsConfiguration config) throws DynamicsException {
        return retryPolicy.execute(() -> {
            StringBuilder url = new StringBuilder(apiBase() + "/" + config.getEntityType());

            // FetchXML mode
            if (config.getFetchXml() != null && !config.getFetchXml().isBlank()) {
                url.append("?fetchXml=").append(
                        URLEncoder.encode(config.getFetchXml(), StandardCharsets.UTF_8));
            } else {
                String separator = "?";
                if (config.getFilter() != null && !config.getFilter().isBlank()) {
                    url.append(separator).append("$filter=").append(
                            URLEncoder.encode(config.getFilter(), StandardCharsets.UTF_8));
                    separator = "&";
                }
                if (config.getSelectFields() != null && !config.getSelectFields().isBlank()) {
                    url.append(separator).append("$select=").append(
                            URLEncoder.encode(config.getSelectFields(), StandardCharsets.UTF_8));
                    separator = "&";
                }
                if (config.getOrderBy() != null && !config.getOrderBy().isBlank()) {
                    url.append(separator).append("$orderby=").append(
                            URLEncoder.encode(config.getOrderBy(), StandardCharsets.UTF_8));
                    separator = "&";
                }
                if (config.getTop() != null && config.getTop() > 0) {
                    url.append(separator).append("$top=").append(config.getTop());
                    separator = "&";
                }
                if (Boolean.TRUE.equals(config.getIncludeCount())) {
                    url.append(separator).append("$count=true");
                }
            }

            HttpRequest request = authenticatedRequest(url.toString())
                    .header("Prefer", "odata.include-annotations=\"*\"")
                    .GET()
                    .build();

            HttpResponse<String> response = executeWithAuth(request);

            if (response.statusCode() != 200) {
                handleErrorResponse(response);
            }

            String responseBody = response.body();
            int recordCount = 0;
            Integer totalCount = null;
            String nextLink = null;

            try {
                JsonNode json = objectMapper.readTree(responseBody);
                JsonNode valueArray = json.get("value");
                if (valueArray != null && valueArray.isArray()) {
                    recordCount = valueArray.size();
                }
                if (json.has("@odata.count")) {
                    totalCount = json.get("@odata.count").asInt();
                }
                if (json.has("@odata.nextLink")) {
                    nextLink = json.get("@odata.nextLink").asText();
                }
            } catch (Exception ignored) {
                // Best effort parsing
            }

            return new QueryEntitiesResult(responseBody, recordCount, totalCount, nextLink);
        });
    }

    public ExecuteActionResult executeAction(DynamicsConfiguration config) throws DynamicsException {
        return retryPolicy.execute(() -> {
            String endpoint;
            if (config.getEntityType() != null && !config.getEntityType().isBlank()
                    && config.getEntityId() != null && !config.getEntityId().isBlank()) {
                endpoint = apiBase() + "/" + config.getEntityType()
                        + "(" + config.getEntityId() + ")/Microsoft.Dynamics.CRM." + config.getActionName();
            } else {
                endpoint = apiBase() + "/" + config.getActionName();
            }

            HttpRequest.Builder requestBuilder = authenticatedRequest(endpoint)
                    .header("Content-Type", "application/json");

            String body = config.getActionParameters() != null && !config.getActionParameters().isBlank()
                    ? config.getActionParameters() : "{}";

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = executeWithAuth(request);

            if (response.statusCode() >= 400) {
                handleErrorResponse(response);
            }

            return new ExecuteActionResult(response.body(), response.statusCode());
        });
    }

    public DeleteEntityResult deleteEntity(DynamicsConfiguration config) throws DynamicsException {
        return retryPolicy.execute(() -> {
            String url = apiBase() + "/" + config.getEntityType() + "(" + config.getEntityId() + ")";

            HttpRequest.Builder requestBuilder = authenticatedRequest(url);
            if (config.getIfMatch() != null && !config.getIfMatch().isBlank()) {
                requestBuilder.header("If-Match", config.getIfMatch());
            }

            HttpRequest request = requestBuilder.DELETE().build();

            HttpResponse<String> response = executeWithAuth(request);

            if (response.statusCode() == 404) {
                // Idempotent delete: treat 404 as success
                return new DeleteEntityResult(true);
            }
            if (response.statusCode() != 204) {
                handleErrorResponse(response);
            }

            return new DeleteEntityResult(true);
        });
    }
}

package com.bonitasoft.connectors.dynamics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DynamicsConfiguration {

    // Connection / Auth (Project/Runtime scope)
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String organizationUrl;
    @Builder.Default
    private String apiVersion = "v9.2";
    @Builder.Default
    private int connectTimeout = 30000;
    @Builder.Default
    private int readTimeout = 60000;
    @Builder.Default
    private int maxRetries = 5;

    // Operation parameters
    private String entityType;
    private String entityId;
    private String entityData;
    private Boolean returnRepresentation;
    private String ifMatch;
    private String selectFields;
    private String expandRelations;
    private String filter;
    private String orderBy;
    @Builder.Default
    private Integer top = 50;
    private Boolean includeCount;
    private String fetchXml;

    // Execute Action
    private String actionName;
    private String actionParameters;
}

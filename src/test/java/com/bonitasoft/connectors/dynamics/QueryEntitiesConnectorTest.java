package com.bonitasoft.connectors.dynamics;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class QueryEntitiesConnectorTest {

    @Mock
    private DynamicsClient mockClient;

    private QueryEntitiesConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new QueryEntitiesConnector();
        inputs = new HashMap<>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("organizationUrl", "https://myorg.crm.dynamics.com");
        inputs.put("entityType", "accounts");
    }

    private void injectMockClient() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var clientField = AbstractDynamicsConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        injectMockClient();
        when(mockClient.queryEntities(any())).thenReturn(
                new QueryEntitiesResult("{\"value\":[{\"accountid\":\"1\"}]}", 1, 10, null));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("recordCount")).isEqualTo(1);
        assertThat(outputs.get("totalCount")).isEqualTo(10);
        assertThat(outputs.get("nextLink")).isNull();
    }

    @Test
    void shouldFailValidationWhenEntityTypeMissing() {
        inputs.remove("entityType");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenTenantIdMissing() {
        inputs.remove("tenantId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldHandleClientException() throws Exception {
        injectMockClient();
        when(mockClient.queryEntities(any())).thenThrow(new DynamicsException("Bad Request", 400, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Bad Request");
    }

    @Test
    void shouldApplyDefaultsForOptionalInputs() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractDynamicsConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (DynamicsConfiguration) configField.get(connector);
        assertThat(config.getFilter()).isNull();
        assertThat(config.getSelectFields()).isNull();
        assertThat(config.getOrderBy()).isNull();
        assertThat(config.getTop()).isEqualTo(50);
        assertThat(config.getIncludeCount()).isFalse();
        assertThat(config.getFetchXml()).isNull();
    }

    @Test
    void shouldAcceptAllQueryParameters() throws Exception {
        inputs.put("filter", "name eq 'Test'");
        inputs.put("selectFields", "name,accountid");
        inputs.put("orderBy", "name asc");
        inputs.put("top", 10);
        inputs.put("includeCount", true);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractDynamicsConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (DynamicsConfiguration) configField.get(connector);
        assertThat(config.getFilter()).isEqualTo("name eq 'Test'");
        assertThat(config.getSelectFields()).isEqualTo("name,accountid");
        assertThat(config.getOrderBy()).isEqualTo("name asc");
        assertThat(config.getTop()).isEqualTo(10);
        assertThat(config.getIncludeCount()).isTrue();
    }

    @Test
    void shouldHandlePaginatedResults() throws Exception {
        injectMockClient();
        when(mockClient.queryEntities(any())).thenReturn(
                new QueryEntitiesResult("{\"value\":[],\"@odata.nextLink\":\"https://next\"}", 50, 200, "https://next"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("nextLink")).isEqualTo("https://next");
        assertThat(outputs.get("totalCount")).isEqualTo(200);
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.queryEntities(any())).thenReturn(
                new QueryEntitiesResult("{\"value\":[]}", 0, 0, null));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("responseBody")).isNotNull();
        assertThat(outputs.get("recordCount")).isEqualTo(0);
        assertThat(outputs.get("totalCount")).isEqualTo(0);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        injectMockClient();
        when(mockClient.queryEntities(any())).thenThrow(new RuntimeException("Timeout"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Timeout");
    }
}

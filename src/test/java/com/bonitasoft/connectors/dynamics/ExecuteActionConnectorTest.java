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
class ExecuteActionConnectorTest {

    @Mock
    private DynamicsClient mockClient;

    private ExecuteActionConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new ExecuteActionConnector();
        inputs = new HashMap<>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("organizationUrl", "https://myorg.crm.dynamics.com");
        inputs.put("actionName", "WinOpportunity");
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
        when(mockClient.executeAction(any())).thenReturn(
                new ExecuteActionResult("{\"result\":\"ok\"}", 200));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("responseBody")).isEqualTo("{\"result\":\"ok\"}");
        assertThat(outputs.get("httpStatusCode")).isEqualTo(200);
    }

    @Test
    void shouldFailValidationWhenActionNameMissing() {
        inputs.remove("actionName");
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
    void shouldFailValidationWhenClientSecretMissing() {
        inputs.remove("clientSecret");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldHandleClientException() throws Exception {
        injectMockClient();
        when(mockClient.executeAction(any())).thenThrow(new DynamicsException("Action not found", 404, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Action not found");
    }

    @Test
    void shouldAcceptOptionalEntityBinding() throws Exception {
        inputs.put("entityType", "opportunities");
        inputs.put("entityId", "opp-456");
        inputs.put("actionParameters", "{\"status\":1}");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractDynamicsConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (DynamicsConfiguration) configField.get(connector);
        assertThat(config.getEntityType()).isEqualTo("opportunities");
        assertThat(config.getEntityId()).isEqualTo("opp-456");
        assertThat(config.getActionParameters()).isEqualTo("{\"status\":1}");
    }

    @Test
    void shouldApplyDefaultsForOptionalInputs() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractDynamicsConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (DynamicsConfiguration) configField.get(connector);
        assertThat(config.getEntityType()).isNull();
        assertThat(config.getEntityId()).isNull();
        assertThat(config.getActionParameters()).isNull();
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.executeAction(any())).thenReturn(
                new ExecuteActionResult("{}", 204));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("responseBody")).isNotNull();
        assertThat(outputs.get("httpStatusCode")).isEqualTo(204);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        injectMockClient();
        when(mockClient.executeAction(any())).thenThrow(new RuntimeException("Unexpected error"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Unexpected error");
    }
}

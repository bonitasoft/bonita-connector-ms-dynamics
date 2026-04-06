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
class UpdateEntityConnectorTest {

    @Mock
    private DynamicsClient mockClient;

    private UpdateEntityConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new UpdateEntityConnector();
        inputs = new HashMap<>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("organizationUrl", "https://myorg.crm.dynamics.com");
        inputs.put("entityType", "accounts");
        inputs.put("entityId", "abc-123");
        inputs.put("entityData", "{\"name\":\"Updated Account\"}");
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
        when(mockClient.updateEntity(any())).thenReturn(
                new UpdateEntityResult("abc-123", ""));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("entityId")).isEqualTo("abc-123");
    }

    @Test
    void shouldFailValidationWhenEntityTypeMissing() {
        inputs.remove("entityType");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenEntityIdMissing() {
        inputs.remove("entityId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenEntityDataMissing() {
        inputs.remove("entityData");
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
        when(mockClient.updateEntity(any())).thenThrow(new DynamicsException("ETag mismatch", 412, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("ETag mismatch");
    }

    @Test
    void shouldApplyDefaultsForOptionalInputs() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractDynamicsConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (DynamicsConfiguration) configField.get(connector);
        assertThat(config.getReturnRepresentation()).isFalse();
        assertThat(config.getIfMatch()).isNull();
    }

    @Test
    void shouldAcceptOptionalIfMatch() throws Exception {
        inputs.put("ifMatch", "W/\"12345\"");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractDynamicsConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (DynamicsConfiguration) configField.get(connector);
        assertThat(config.getIfMatch()).isEqualTo("W/\"12345\"");
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.updateEntity(any())).thenReturn(
                new UpdateEntityResult("abc-123", "{\"name\":\"Updated\"}"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("entityId")).isEqualTo("abc-123");
        assertThat(outputs.get("responseBody")).isEqualTo("{\"name\":\"Updated\"}");
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        injectMockClient();
        when(mockClient.updateEntity(any())).thenThrow(new RuntimeException("Unexpected"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Unexpected");
    }
}

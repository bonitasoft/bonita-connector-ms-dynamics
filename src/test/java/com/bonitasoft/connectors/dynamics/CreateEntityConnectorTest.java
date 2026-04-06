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
class CreateEntityConnectorTest {

    @Mock
    private DynamicsClient mockClient;

    private CreateEntityConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new CreateEntityConnector();
        inputs = new HashMap<>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("organizationUrl", "https://myorg.crm.dynamics.com");
        inputs.put("entityType", "accounts");
        inputs.put("entityData", "{\"name\":\"Test Account\"}");
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
        when(mockClient.createEntity(any())).thenReturn(
                new CreateEntityResult("abc-123", "https://myorg.crm.dynamics.com/api/data/v9.2/accounts(abc-123)", "{\"accountid\":\"abc-123\"}"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("entityId")).isEqualTo("abc-123");
        assertThat(outputs.get("entityUrl")).isEqualTo("https://myorg.crm.dynamics.com/api/data/v9.2/accounts(abc-123)");
        assertThat(outputs.get("responseBody")).isEqualTo("{\"accountid\":\"abc-123\"}");
    }

    @Test
    void shouldFailValidationWhenTenantIdMissing() {
        inputs.remove("tenantId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenClientIdMissing() {
        inputs.remove("clientId");
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
    void shouldFailValidationWhenOrganizationUrlMissing() {
        inputs.remove("organizationUrl");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenEntityTypeMissing() {
        inputs.remove("entityType");
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
    void shouldHandleClientException() throws Exception {
        injectMockClient();
        when(mockClient.createEntity(any())).thenThrow(new DynamicsException("Forbidden: insufficient privileges", 403, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Forbidden");
    }

    @Test
    void shouldSetErrorOutputsOnUnexpectedException() throws Exception {
        injectMockClient();
        when(mockClient.createEntity(any())).thenThrow(new RuntimeException("Network failure"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Network failure");
    }

    @Test
    void shouldApplyDefaultsForOptionalInputs() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractDynamicsConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (DynamicsConfiguration) configField.get(connector);
        assertThat(config.getApiVersion()).isEqualTo("v9.2");
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
        assertThat(config.getReturnRepresentation()).isFalse();
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.createEntity(any())).thenReturn(
                new CreateEntityResult("id-1", "https://url", "{\"body\":true}"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("entityId")).isEqualTo("id-1");
        assertThat(outputs.get("entityUrl")).isEqualTo("https://url");
        assertThat(outputs.get("responseBody")).isEqualTo("{\"body\":true}");
    }
}

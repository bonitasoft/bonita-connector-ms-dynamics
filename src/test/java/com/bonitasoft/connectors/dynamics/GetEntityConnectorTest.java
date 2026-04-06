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
class GetEntityConnectorTest {

    @Mock
    private DynamicsClient mockClient;

    private GetEntityConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new GetEntityConnector();
        inputs = new HashMap<>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("organizationUrl", "https://myorg.crm.dynamics.com");
        inputs.put("entityType", "accounts");
        inputs.put("entityId", "abc-123");
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
        when(mockClient.getEntity(any())).thenReturn(
                new GetEntityResult("abc-123", "{\"accountid\":\"abc-123\",\"name\":\"Test\"}", "W/\"12345\""));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("entityId")).isEqualTo("abc-123");
        assertThat(outputs.get("responseBody")).isEqualTo("{\"accountid\":\"abc-123\",\"name\":\"Test\"}");
        assertThat(outputs.get("etag")).isEqualTo("W/\"12345\"");
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
    void shouldFailValidationWhenOrganizationUrlMissing() {
        inputs.remove("organizationUrl");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldHandleNotFound() throws Exception {
        injectMockClient();
        when(mockClient.getEntity(any())).thenThrow(new DynamicsException("Entity not found: abc-123", 404, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Entity not found");
    }

    @Test
    void shouldApplyDefaultsForOptionalInputs() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractDynamicsConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (DynamicsConfiguration) configField.get(connector);
        assertThat(config.getSelectFields()).isNull();
        assertThat(config.getExpandRelations()).isNull();
    }

    @Test
    void shouldAcceptOptionalSelectAndExpand() throws Exception {
        inputs.put("selectFields", "name,accountid");
        inputs.put("expandRelations", "primarycontactid");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var configField = AbstractDynamicsConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (DynamicsConfiguration) configField.get(connector);
        assertThat(config.getSelectFields()).isEqualTo("name,accountid");
        assertThat(config.getExpandRelations()).isEqualTo("primarycontactid");
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.getEntity(any())).thenReturn(
                new GetEntityResult("abc-123", "{}", "W/\"etag\""));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("entityId")).isEqualTo("abc-123");
        assertThat(outputs.get("responseBody")).isNotNull();
        assertThat(outputs.get("etag")).isEqualTo("W/\"etag\"");
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        injectMockClient();
        when(mockClient.getEntity(any())).thenThrow(new RuntimeException("Connection reset"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Connection reset");
    }
}

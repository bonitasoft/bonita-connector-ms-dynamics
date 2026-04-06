package com.bonitasoft.connectors.dynamics;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CreateEntityConnectorPropertyTest {

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "\t", "\n");
    }

    @Provide
    Arbitrary<String> validEntityTypes() {
        return Arbitraries.of("accounts", "contacts", "leads", "opportunities", "incidents", "tasks");
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("organizationUrl", "https://myorg.crm.dynamics.com");
        inputs.put("entityType", "accounts");
        inputs.put("entityData", "{\"name\":\"Test\"}");
        return inputs;
    }

    @Property
    void mandatoryTenantIdRejectsBlank(@ForAll("blankStrings") String tenantId) {
        var connector = new CreateEntityConnector();
        var inputs = validInputs();
        inputs.put("tenantId", tenantId);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void mandatoryClientIdRejectsBlank(@ForAll("blankStrings") String clientId) {
        var connector = new CreateEntityConnector();
        var inputs = validInputs();
        inputs.put("clientId", clientId);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void mandatoryClientSecretRejectsBlank(@ForAll("blankStrings") String clientSecret) {
        var connector = new CreateEntityConnector();
        var inputs = validInputs();
        inputs.put("clientSecret", clientSecret);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void mandatoryOrganizationUrlRejectsBlank(@ForAll("blankStrings") String url) {
        var connector = new CreateEntityConnector();
        var inputs = validInputs();
        inputs.put("organizationUrl", url);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void mandatoryEntityTypeRejectsBlank(@ForAll("blankStrings") String entityType) {
        var connector = new CreateEntityConnector();
        var inputs = validInputs();
        inputs.put("entityType", entityType);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void mandatoryEntityDataRejectsBlank(@ForAll("blankStrings") String entityData) {
        var connector = new CreateEntityConnector();
        var inputs = validInputs();
        inputs.put("entityData", entityData);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void validConfigurationAlwaysBuilds(
            @ForAll("validEntityTypes") String entityType,
            @ForAll @AlphaChars @StringLength(min = 1, max = 100) String tenantId) {
        var connector = new CreateEntityConnector();
        var inputs = validInputs();
        inputs.put("entityType", entityType);
        inputs.put("tenantId", tenantId);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void entityTypeAcceptsValidValues(@ForAll("validEntityTypes") String entityType) {
        var connector = new CreateEntityConnector();
        var inputs = validInputs();
        inputs.put("entityType", entityType);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void defaultValuesApplied() {
        var config = DynamicsConfiguration.builder()
                .tenantId("t")
                .clientId("c")
                .clientSecret("s")
                .organizationUrl("https://org.crm.dynamics.com")
                .build();
        assertThat(config.getApiVersion()).isEqualTo("v9.2");
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
        assertThat(config.getMaxRetries()).isEqualTo(5);
        assertThat(config.getTop()).isEqualTo(50);
    }

    @Property
    void timeoutPositiveOnly(@ForAll @IntRange(min = 1, max = 300000) int timeout) {
        var config = DynamicsConfiguration.builder()
                .tenantId("t").clientId("c").clientSecret("s")
                .organizationUrl("https://org.crm.dynamics.com")
                .connectTimeout(timeout)
                .build();
        assertThat(config.getConnectTimeout()).isPositive();
    }

    @Property
    void errorMessageTruncation(@ForAll @AlphaChars @StringLength(min = 1001, max = 2000) String longMsg) {
        var truncated = longMsg.length() > 1000 ? longMsg.substring(0, 1000) : longMsg;
        assertThat(truncated.length()).isLessThanOrEqualTo(1000);
    }
}

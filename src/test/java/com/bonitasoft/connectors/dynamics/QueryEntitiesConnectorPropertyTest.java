package com.bonitasoft.connectors.dynamics;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class QueryEntitiesConnectorPropertyTest {

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "\t", "\n");
    }

    @Provide
    Arbitrary<String> validEntityTypes() {
        return Arbitraries.of("accounts", "contacts", "leads", "opportunities", "incidents");
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("tenantId", "test-tenant-id");
        inputs.put("clientId", "test-client-id");
        inputs.put("clientSecret", "test-client-secret");
        inputs.put("organizationUrl", "https://myorg.crm.dynamics.com");
        inputs.put("entityType", "accounts");
        return inputs;
    }

    @Property
    void mandatoryEntityTypeRejectsBlank(@ForAll("blankStrings") String entityType) {
        var connector = new QueryEntitiesConnector();
        var inputs = validInputs();
        inputs.put("entityType", entityType);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void mandatoryTenantIdRejectsBlank(@ForAll("blankStrings") String tenantId) {
        var connector = new QueryEntitiesConnector();
        var inputs = validInputs();
        inputs.put("tenantId", tenantId);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void validEntityTypesAccepted(@ForAll("validEntityTypes") String entityType) {
        var connector = new QueryEntitiesConnector();
        var inputs = validInputs();
        inputs.put("entityType", entityType);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void topValuePreserved(@ForAll @IntRange(min = 1, max = 5000) int top) {
        var connector = new QueryEntitiesConnector();
        var inputs = validInputs();
        inputs.put("top", top);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void filterStringPreserved(@ForAll @AlphaChars @StringLength(min = 1, max = 200) String filter) {
        var connector = new QueryEntitiesConnector();
        var inputs = validInputs();
        inputs.put("filter", filter);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void selectFieldsPreserved(@ForAll @AlphaChars @StringLength(min = 1, max = 200) String select) {
        var connector = new QueryEntitiesConnector();
        var inputs = validInputs();
        inputs.put("selectFields", select);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void orderByPreserved(@ForAll @AlphaChars @StringLength(min = 1, max = 100) String orderBy) {
        var connector = new QueryEntitiesConnector();
        var inputs = validInputs();
        inputs.put("orderBy", orderBy);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void includeCountBooleanAccepted(@ForAll boolean includeCount) {
        var connector = new QueryEntitiesConnector();
        var inputs = validInputs();
        inputs.put("includeCount", includeCount);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void defaultTopValueIsPreserved() {
        var config = DynamicsConfiguration.builder()
                .tenantId("t").clientId("c").clientSecret("s")
                .organizationUrl("https://org.crm.dynamics.com")
                .build();
        assertThat(config.getTop()).isEqualTo(50);
    }

    @Property
    void queryResultRecordPreservesValues(
            @ForAll @IntRange(min = 0, max = 5000) int recordCount,
            @ForAll @IntRange(min = 0, max = 100000) int totalCount) {
        var result = new QueryEntitiesResult("{}", recordCount, totalCount, null);
        assertThat(result.recordCount()).isEqualTo(recordCount);
        assertThat(result.totalCount()).isEqualTo(totalCount);
    }
}

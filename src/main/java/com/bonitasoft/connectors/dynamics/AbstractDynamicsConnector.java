package com.bonitasoft.connectors.dynamics;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.Map;

@Slf4j
public abstract class AbstractDynamicsConnector extends AbstractConnector {

    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    protected DynamicsConfiguration configuration;
    protected DynamicsClient client;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            this.configuration = buildConfiguration();
            validateConfiguration(this.configuration);
        } catch (IllegalArgumentException e) {
            throw new ConnectorValidationException(this, e.getMessage());
        }
    }

    @Override
    public void connect() throws ConnectorException {
        try {
            this.client = new DynamicsClient(this.configuration);
            log.info("Dynamics 365 connector connected successfully");
        } catch (DynamicsException e) {
            throw new ConnectorException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() throws ConnectorException {
        this.client = null;
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (DynamicsException e) {
            log.error("Dynamics 365 connector execution failed: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, truncate(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error in Dynamics 365 connector: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, truncate("Unexpected error: " + e.getMessage()));
        }
    }

    protected abstract void doExecute() throws DynamicsException;

    protected abstract DynamicsConfiguration buildConfiguration();

    protected void validateConfiguration(DynamicsConfiguration config) {
        if (config.getTenantId() == null || config.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId is mandatory");
        }
        if (config.getClientId() == null || config.getClientId().isBlank()) {
            throw new IllegalArgumentException("clientId is mandatory");
        }
        if (config.getClientSecret() == null || config.getClientSecret().isBlank()) {
            throw new IllegalArgumentException("clientSecret is mandatory");
        }
        if (config.getOrganizationUrl() == null || config.getOrganizationUrl().isBlank()) {
            throw new IllegalArgumentException("organizationUrl is mandatory");
        }
    }

    protected String readStringInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? value.toString() : null;
    }

    protected String readStringInput(String name, String defaultValue) {
        String value = readStringInput(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    protected Boolean readBooleanInput(String name, boolean defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : defaultValue;
    }

    protected Integer readIntegerInput(String name, int defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : defaultValue;
    }

    protected DynamicsConfiguration.DynamicsConfigurationBuilder connectionConfig() {
        return DynamicsConfiguration.builder()
                .tenantId(readStringInput("tenantId"))
                .clientId(readStringInput("clientId"))
                .clientSecret(readStringInput("clientSecret"))
                .organizationUrl(readStringInput("organizationUrl"))
                .apiVersion(readStringInput("apiVersion", "v9.2"))
                .connectTimeout(readIntegerInput("connectTimeout", 30000))
                .readTimeout(readIntegerInput("readTimeout", 60000));
    }

    private String truncate(String message) {
        if (message == null) return "";
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    Map<String, Object> getOutputs() {
        return getOutputParameters();
    }
}

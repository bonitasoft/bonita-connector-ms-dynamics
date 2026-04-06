package com.bonitasoft.connectors.dynamics;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteActionConnector extends AbstractDynamicsConnector {

    static final String INPUT_ACTION_NAME = "actionName";
    static final String INPUT_ENTITY_TYPE = "entityType";
    static final String INPUT_ENTITY_ID = "entityId";
    static final String INPUT_ACTION_PARAMETERS = "actionParameters";

    static final String OUTPUT_RESPONSE_BODY = "responseBody";
    static final String OUTPUT_HTTP_STATUS_CODE = "httpStatusCode";

    @Override
    protected DynamicsConfiguration buildConfiguration() {
        return connectionConfig()
                .actionName(readStringInput(INPUT_ACTION_NAME))
                .entityType(readStringInput(INPUT_ENTITY_TYPE))
                .entityId(readStringInput(INPUT_ENTITY_ID))
                .actionParameters(readStringInput(INPUT_ACTION_PARAMETERS))
                .build();
    }

    @Override
    protected void validateConfiguration(DynamicsConfiguration config) {
        super.validateConfiguration(config);
        if (config.getActionName() == null || config.getActionName().isBlank()) {
            throw new IllegalArgumentException("actionName is mandatory");
        }
    }

    @Override
    protected void doExecute() throws DynamicsException {
        log.info("Executing action {}", configuration.getActionName());
        ExecuteActionResult result = client.executeAction(configuration);
        setOutputParameter(OUTPUT_RESPONSE_BODY, result.responseBody());
        setOutputParameter(OUTPUT_HTTP_STATUS_CODE, result.httpStatusCode());
        log.info("Action {} executed with status {}", configuration.getActionName(), result.httpStatusCode());
    }
}

package com.bonitasoft.connectors.dynamics;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateEntityConnector extends AbstractDynamicsConnector {

    static final String INPUT_ENTITY_TYPE = "entityType";
    static final String INPUT_ENTITY_ID = "entityId";
    static final String INPUT_ENTITY_DATA = "entityData";
    static final String INPUT_RETURN_REPRESENTATION = "returnRepresentation";
    static final String INPUT_IF_MATCH = "ifMatch";

    static final String OUTPUT_ENTITY_ID = "entityId";
    static final String OUTPUT_RESPONSE_BODY = "responseBody";

    @Override
    protected DynamicsConfiguration buildConfiguration() {
        return connectionConfig()
                .entityType(readStringInput(INPUT_ENTITY_TYPE))
                .entityId(readStringInput(INPUT_ENTITY_ID))
                .entityData(readStringInput(INPUT_ENTITY_DATA))
                .returnRepresentation(readBooleanInput(INPUT_RETURN_REPRESENTATION, false))
                .ifMatch(readStringInput(INPUT_IF_MATCH))
                .build();
    }

    @Override
    protected void validateConfiguration(DynamicsConfiguration config) {
        super.validateConfiguration(config);
        if (config.getEntityType() == null || config.getEntityType().isBlank()) {
            throw new IllegalArgumentException("entityType is mandatory");
        }
        if (config.getEntityId() == null || config.getEntityId().isBlank()) {
            throw new IllegalArgumentException("entityId is mandatory");
        }
        if (config.getEntityData() == null || config.getEntityData().isBlank()) {
            throw new IllegalArgumentException("entityData is mandatory");
        }
    }

    @Override
    protected void doExecute() throws DynamicsException {
        log.info("Updating entity {} in {}", configuration.getEntityId(), configuration.getEntityType());
        UpdateEntityResult result = client.updateEntity(configuration);
        setOutputParameter(OUTPUT_ENTITY_ID, result.entityId());
        setOutputParameter(OUTPUT_RESPONSE_BODY, result.responseBody());
        log.info("Entity updated: {}", result.entityId());
    }
}

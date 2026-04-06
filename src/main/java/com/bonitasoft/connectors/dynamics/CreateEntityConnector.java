package com.bonitasoft.connectors.dynamics;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateEntityConnector extends AbstractDynamicsConnector {

    static final String INPUT_ENTITY_TYPE = "entityType";
    static final String INPUT_ENTITY_DATA = "entityData";
    static final String INPUT_RETURN_REPRESENTATION = "returnRepresentation";

    static final String OUTPUT_ENTITY_ID = "entityId";
    static final String OUTPUT_ENTITY_URL = "entityUrl";
    static final String OUTPUT_RESPONSE_BODY = "responseBody";

    @Override
    protected DynamicsConfiguration buildConfiguration() {
        return connectionConfig()
                .entityType(readStringInput(INPUT_ENTITY_TYPE))
                .entityData(readStringInput(INPUT_ENTITY_DATA))
                .returnRepresentation(readBooleanInput(INPUT_RETURN_REPRESENTATION, false))
                .build();
    }

    @Override
    protected void validateConfiguration(DynamicsConfiguration config) {
        super.validateConfiguration(config);
        if (config.getEntityType() == null || config.getEntityType().isBlank()) {
            throw new IllegalArgumentException("entityType is mandatory");
        }
        if (config.getEntityData() == null || config.getEntityData().isBlank()) {
            throw new IllegalArgumentException("entityData is mandatory");
        }
    }

    @Override
    protected void doExecute() throws DynamicsException {
        log.info("Creating entity in {}", configuration.getEntityType());
        CreateEntityResult result = client.createEntity(configuration);
        setOutputParameter(OUTPUT_ENTITY_ID, result.entityId());
        setOutputParameter(OUTPUT_ENTITY_URL, result.entityUrl());
        setOutputParameter(OUTPUT_RESPONSE_BODY, result.responseBody());
        log.info("Entity created: {}", result.entityId());
    }
}

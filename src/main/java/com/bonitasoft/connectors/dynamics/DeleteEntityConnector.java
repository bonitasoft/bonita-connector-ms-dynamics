package com.bonitasoft.connectors.dynamics;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteEntityConnector extends AbstractDynamicsConnector {

    static final String INPUT_ENTITY_TYPE = "entityType";
    static final String INPUT_ENTITY_ID = "entityId";
    static final String INPUT_IF_MATCH = "ifMatch";

    @Override
    protected DynamicsConfiguration buildConfiguration() {
        return connectionConfig()
                .entityType(readStringInput(INPUT_ENTITY_TYPE))
                .entityId(readStringInput(INPUT_ENTITY_ID))
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
    }

    @Override
    protected void doExecute() throws DynamicsException {
        log.info("Deleting entity {} from {}", configuration.getEntityId(), configuration.getEntityType());
        client.deleteEntity(configuration);
        log.info("Entity deleted: {}", configuration.getEntityId());
    }
}

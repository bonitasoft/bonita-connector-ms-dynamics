package com.bonitasoft.connectors.dynamics;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetEntityConnector extends AbstractDynamicsConnector {

    static final String INPUT_ENTITY_TYPE = "entityType";
    static final String INPUT_ENTITY_ID = "entityId";
    static final String INPUT_SELECT_FIELDS = "selectFields";
    static final String INPUT_EXPAND_RELATIONS = "expandRelations";

    static final String OUTPUT_ENTITY_ID = "entityId";
    static final String OUTPUT_RESPONSE_BODY = "responseBody";
    static final String OUTPUT_ETAG = "etag";

    @Override
    protected DynamicsConfiguration buildConfiguration() {
        return connectionConfig()
                .entityType(readStringInput(INPUT_ENTITY_TYPE))
                .entityId(readStringInput(INPUT_ENTITY_ID))
                .selectFields(readStringInput(INPUT_SELECT_FIELDS))
                .expandRelations(readStringInput(INPUT_EXPAND_RELATIONS))
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
        log.info("Getting entity {} from {}", configuration.getEntityId(), configuration.getEntityType());
        GetEntityResult result = client.getEntity(configuration);
        setOutputParameter(OUTPUT_ENTITY_ID, result.entityId());
        setOutputParameter(OUTPUT_RESPONSE_BODY, result.responseBody());
        setOutputParameter(OUTPUT_ETAG, result.etag());
        log.info("Entity retrieved: {}", result.entityId());
    }
}

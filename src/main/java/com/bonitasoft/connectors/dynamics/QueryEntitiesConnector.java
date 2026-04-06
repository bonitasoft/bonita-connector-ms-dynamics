package com.bonitasoft.connectors.dynamics;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryEntitiesConnector extends AbstractDynamicsConnector {

    static final String INPUT_ENTITY_TYPE = "entityType";
    static final String INPUT_FILTER = "filter";
    static final String INPUT_SELECT_FIELDS = "selectFields";
    static final String INPUT_ORDER_BY = "orderBy";
    static final String INPUT_TOP = "top";
    static final String INPUT_INCLUDE_COUNT = "includeCount";
    static final String INPUT_FETCH_XML = "fetchXml";

    static final String OUTPUT_RESPONSE_BODY = "responseBody";
    static final String OUTPUT_RECORD_COUNT = "recordCount";
    static final String OUTPUT_TOTAL_COUNT = "totalCount";
    static final String OUTPUT_NEXT_LINK = "nextLink";

    @Override
    protected DynamicsConfiguration buildConfiguration() {
        return connectionConfig()
                .entityType(readStringInput(INPUT_ENTITY_TYPE))
                .filter(readStringInput(INPUT_FILTER))
                .selectFields(readStringInput(INPUT_SELECT_FIELDS))
                .orderBy(readStringInput(INPUT_ORDER_BY))
                .top(readIntegerInput(INPUT_TOP, 50))
                .includeCount(readBooleanInput(INPUT_INCLUDE_COUNT, false))
                .fetchXml(readStringInput(INPUT_FETCH_XML))
                .build();
    }

    @Override
    protected void validateConfiguration(DynamicsConfiguration config) {
        super.validateConfiguration(config);
        if (config.getEntityType() == null || config.getEntityType().isBlank()) {
            throw new IllegalArgumentException("entityType is mandatory");
        }
    }

    @Override
    protected void doExecute() throws DynamicsException {
        log.info("Querying entities from {}", configuration.getEntityType());
        QueryEntitiesResult result = client.queryEntities(configuration);
        setOutputParameter(OUTPUT_RESPONSE_BODY, result.responseBody());
        setOutputParameter(OUTPUT_RECORD_COUNT, result.recordCount());
        setOutputParameter(OUTPUT_TOTAL_COUNT, result.totalCount());
        setOutputParameter(OUTPUT_NEXT_LINK, result.nextLink());
        log.info("Query returned {} records", result.recordCount());
    }
}

package com.bonitasoft.connectors.dynamics;

public record QueryEntitiesResult(String responseBody, Integer recordCount, Integer totalCount, String nextLink) {}

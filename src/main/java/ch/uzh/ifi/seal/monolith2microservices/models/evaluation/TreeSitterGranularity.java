package ch.uzh.ifi.seal.monolith2microservices.models.evaluation;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TreeSitterGranularity {
    CLASS,
    FUNCTION,
    MODULE;

    @JsonCreator
    public static TreeSitterGranularity from(String value) {
        return TreeSitterGranularity.valueOf(value.toUpperCase());
    }
}

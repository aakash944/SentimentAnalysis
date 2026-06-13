package com.example.demo.sentiment_analysis.reaction.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReactionType {
    HAHA,
    LIKE,
    SAD,
    HAPPY,
    LOVE;

    @JsonCreator
    public static ReactionType fromString(String value) {
        try {
            return ReactionType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid reaction type: " + value);
        }
    }
}

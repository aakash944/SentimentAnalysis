package com.example.demo.sentiment_analysis.exception;

public class GibberishCommentException extends RuntimeException {
    public GibberishCommentException(String message) {
        super(message);
    }
}

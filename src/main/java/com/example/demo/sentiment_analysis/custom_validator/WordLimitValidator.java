package com.example.demo.sentiment_analysis.custom_validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WordLimitValidator
        implements ConstraintValidator<WordLimit, String> {
        private int max;
        @Override
        public void initialize(WordLimit annotation) {
            this.max = annotation.max();
        }
        @Override
        public boolean isValid(String value,
                               ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            return value
                    .trim()
                    .split("\\s+")
                    .length <= max;
        }
}

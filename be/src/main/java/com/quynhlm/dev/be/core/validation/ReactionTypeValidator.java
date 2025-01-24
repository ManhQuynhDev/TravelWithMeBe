package com.quynhlm.dev.be.core.validation;

import java.util.Arrays;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReactionTypeValidator implements ConstraintValidator<ValidReactionType, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String[] reactionType = { "LIKE", "LOVE", "HAHA", "WOW", "SAD", "ANGRY" };

        if (value == null) {
            return false;
        }

        return Arrays.asList(reactionType).contains(value);
    }
}

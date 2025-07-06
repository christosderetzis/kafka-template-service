package org.kafka.template.kafkatemplateservice.utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

@Slf4j
@Component
public class ValidatorUtils {
    private final Validator validator;

    public ValidatorUtils(Validator validator) {
        this.validator = validator;
    }

    public <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Validation failed", violations);
        }
    }
}

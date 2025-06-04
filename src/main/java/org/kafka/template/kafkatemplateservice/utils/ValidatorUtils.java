package org.kafka.template.kafkatemplateservice.utils;

import lombok.extern.slf4j.Slf4j;

import javax.validation.*;
import java.util.Set;

@Slf4j
public class ValidatorUtils {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    public static <T> void validateSchema(T message) {
        Set<ConstraintViolation<T>> validationErrors = VALIDATOR.validate(message);
        if (!validationErrors.isEmpty()) {
            throw new ConstraintViolationException(validationErrors);
        }
    }
}

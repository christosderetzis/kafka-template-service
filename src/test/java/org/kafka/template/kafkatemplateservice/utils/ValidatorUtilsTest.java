package org.kafka.template.kafkatemplateservice.utils;

import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kafka.template.kafkatemplateservice.models.User;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ValidatorUtilsTest {

    private ValidatorUtils validatorUtils;
    private Validator validator;

    @BeforeEach
    void setUp() {
        // Use Spring's Validator implementation
        validator = new LocalValidatorFactoryBean();
        ((LocalValidatorFactoryBean) validator).afterPropertiesSet(); // Initialize the Validator

        // Initialize ValidatorUtils with the real Validator
        validatorUtils = new ValidatorUtils(validator);
    }

    @Test
    void validate_ShouldPass_WhenValidUser() {
        // Arrange
        User validUser = User.builder()
                .id(1)
                .name("John Doe")
                .email("john.doe@example.com")
                .age(30)
                .build();

        // Act & Assert
        // Validation should pass without throwing any exception
        assertDoesNotThrow(() -> validatorUtils.validate(validUser));
    }

    @Test
    void validate_ShouldThrowConstraintViolationException_WhenInvalidUser() {
        // Arrange
        User invalidUser = User.builder()
                .id(null)  // ID is invalid (null)
                .name(null)  // Name is invalid (null)
                .email("john.doe@example.com")
                .age(30)
                .build();

        // Act & Assert
        // Validation should throw a ConstraintViolationException
        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> {
            validatorUtils.validate(invalidUser);
        });

        // Assert that the exception message is correct
        assertEquals("Validation failed", exception.getMessage());
    }
}


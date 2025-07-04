package org.kafka.template.kafkatemplateservice.specs;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kafka.template.kafkatemplateservice.base.BaseKafkaFunctionalSpec;
import org.kafka.template.kafkatemplateservice.controllers.UserController;

import static org.awaitility.Awaitility.await;

class UserCreatedFunctionalTest extends BaseKafkaFunctionalSpec {

    @Test
    void givenInvalidUser_SchemaValidationWillFail_OnProducerSide() throws Exception {
        // Given we have a user with invalid data
        UserController.UserDto userDto = UserController.UserDto
                .builder()
                .id(1)
                .build();

        // When we try to create the user
        webActor.createUser(userDto);

        // Then we expect the schema validation to fail
        await().untilAsserted(() -> {
            Assertions.assertTrue(assertLog(Level.ERROR, "Schema validation failed: Error serializing JSON message"));
        });
    }

    @Test
    void givenValidUser_SchemaValidationWillPass_OnProducerSide() throws Exception {
        // Given we have a user with valid data
        UserController.UserDto userDto = UserController.UserDto
                .builder()
                .id(1)
                .name("John Doe")
                .email("john.doe@mail.com")
                .age(30)
                .build();

        // When we try to create the user
        webActor.createUser(userDto);

        // Then we expect the schema validation to pass
        await().untilAsserted(() -> {
            Assertions.assertTrue(assertLog(Level.INFO, "User sent successfully: User(id=1, name=John Doe, email=john.doe@mail.com, age=30)"));
            Assertions.assertTrue(assertLog(Level.INFO, "Consumed valid user: User(id=1, name=John Doe, email=john.doe@mail.com, age=30)"));
        });
    }

    @Test
    void givenUnderageUser_WarningWillBeLogged_OnConsumerSide() throws Exception {
        // Given we have an underage user
        UserController.UserDto userDto = UserController.UserDto
                .builder()
                .id(3)
                .name("Jane Doe")
                .age(15)
                .build();


        // When we try to create the user
        webActor.createUser(userDto);

        // Then we expect a warning log for underage user
        await().untilAsserted(() -> {
            Assertions.assertTrue(assertLog(Level.WARN, "Underage user detected: User(id=3, name=Jane Doe, email=null, age=15)"));
        });
    }
}

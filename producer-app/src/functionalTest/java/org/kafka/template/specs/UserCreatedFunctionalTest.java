package org.kafka.template.specs;

import ch.qos.logback.classic.Level;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kafka.template.dtos.GenericResponseDto;
import org.kafka.template.dtos.UserCreatedRequestDto;
import org.kafka.template.base.BaseKafkaFunctionalSpec;
import org.kafka.template.enums.GenericResponseStatus;
import org.kafka.template.models.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class UserCreatedFunctionalTest extends BaseKafkaFunctionalSpec {

    @Test
    void givenInvalidUser_SchemaValidationWillFail_OnProducerSide() throws Exception {
        // Given we have a user with invalid data
        UserCreatedRequestDto userDto = UserCreatedRequestDto
                .builder()
                .id(1)
                .build();

        // When we try to create the user
        WebTestClient.ResponseSpec response = webActor.createUser(userDto);

        response.expectStatus().is5xxServerError();

        // Then we expect the schema validation to fail
        await().untilAsserted(() -> {
            Assertions.assertTrue(assertLog(Level.ERROR, "Schema validation failed: Error serializing JSON message"));
        });
    }

    @Test
    void givenValidUser_SchemaValidationWillPass_OnProducerSide() throws Exception {
        // Given we have a user with valid data
        UserCreatedRequestDto userDto = UserCreatedRequestDto
                .builder()
                .id(1)
                .name("John Doe")
                .email("john.doe@mail.com")
                .age(30)
                .build();

        // When we try to create the user
        WebTestClient.ResponseSpec response = webActor.createUser(userDto);
        GenericResponseDto<UserCreatedRequestDto> responseBody = response
                .expectStatus().is2xxSuccessful()
                .expectBody(new ParameterizedTypeReference<GenericResponseDto<UserCreatedRequestDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // 200 OK
        response.expectStatus().is2xxSuccessful();

        // And we expect a success response
        assertNotNull(responseBody);
        assertEquals(GenericResponseStatus.SUCCESS, responseBody.getStatus());

        // Then we expect the schema validation to pass
        await().untilAsserted(() -> {
            Assertions.assertTrue(assertLog(Level.INFO, "User sent successfully with key:"));
            Assertions.assertTrue(assertLog(Level.INFO, "and value: User(id=1, name=John Doe, email=john.doe@mail.com, age=30)"));
        });

        // Additionally, verify that the consumer also logs the valid user consumption
        List<ConsumerRecord<String, String>> messages = kafkaActor.consume(1, "user-created");
        assertEquals(1, messages.size(), "Expected exactly 1 message to be consumed");

        // Assert the consumed message content
        ConsumerRecord<String, String> record = messages.getFirst();

        // Verify the key is a valid UUID
        assertNotNull(record.key(), "Message key should not be null");
        assertDoesNotThrow(() -> UUID.fromString(record.key()), "Message key should be a valid UUID");

        // Parse and verify the message value (KafkaJsonSchemaDeserializer returns LinkedHashMap)
        User consumedUser = OBJECT_MAPPER.convertValue(record.value(), User.class);

        assertEquals(1, consumedUser.getId(), "User ID should match");
        assertEquals("John Doe", consumedUser.getName(), "User name should match");
        assertEquals("john.doe@mail.com", consumedUser.getEmail(), "User email should match");
        assertEquals(30, consumedUser.getAge(), "User age should match");
    }
}

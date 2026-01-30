package org.kafka.template.specs;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kafka.template.base.BaseKafkaFunctionalSpec;
import org.kafka.template.entity.UserEntity;
import org.kafka.template.models.User;

import java.util.UUID;

import static org.awaitility.Awaitility.await;

class UserConsumerFunctionalTest extends BaseKafkaFunctionalSpec {

    @Test
    void givenValidUser_consumerWillConsumeIt() throws Exception {
        // Given we have a user with valid data
        User userDto = User
                .builder()
                .id(1)
                .name("John Doe")
                .email("john.doe@mail.com")
                .age(30)
                .build();

        // When we try to create the user
        kafkaActor.produce(UUID.randomUUID().toString(), userDto, "user-created");

        // Then we expect the schema validation to pass
        await().untilAsserted(() -> {
            Assertions.assertTrue(assertLog(Level.INFO, "Consumed valid user: User(id=1, name=John Doe, email=john.doe@mail.com, age=30)"));
        });

        // And we expect the user to be saved in the database
        UserEntity savedUser = userRepository.findAll().get(0);

        Assertions.assertNotNull(savedUser);
        Assertions.assertEquals(1, savedUser.getUserId());
        Assertions.assertEquals("john.doe@mail.com", savedUser.getEmail());
        Assertions.assertEquals(30, savedUser.getAge());
        Assertions.assertEquals("John Doe", savedUser.getName());
    }

    @Test
    void givenUnderageUser_WarningWillBeLogged_OnConsumerSide() throws Exception {
        // Given we have an underage user
        User user = User
                .builder()
                .id(3)
                .name("Jane Doe")
                .age(15)
                .build();


        // When we produce the user to the Kafka topic
        kafkaActor.produce(UUID.randomUUID().toString(), user, "user-created");

        // Then we expect a warning log for underage user
        await().untilAsserted(() -> {
            Assertions.assertTrue(assertLog(Level.WARN, "Underage user detected: User(id=3, name=Jane Doe, email=null, age=15)"));
        });

        // And we expect the user to be saved in the database
        UserEntity savedUser = userRepository.findAll().get(0);

        Assertions.assertNotNull(savedUser);
        Assertions.assertEquals(3, savedUser.getUserId());
        Assertions.assertNull(savedUser.getEmail());
        Assertions.assertEquals(15, savedUser.getAge());
        Assertions.assertEquals("Jane Doe", savedUser.getName());
    }

    @Test
    void givenInvalidUserPayload_SchemaValidationWillFail_OnConsumerSide() throws Exception {
        // given we have a user with invalid data
        User user = User.builder()
                .id(4)
                .build();

        // when we produce the user to the Kafka topic
        kafkaActor.produce(UUID.randomUUID().toString(), user, "user-created");

        // then we expect the schema validation to fail
        await().untilAsserted(() -> {
            Assertions.assertTrue(assertLog(Level.ERROR, "Invalid user payload received:"));
        });
        Assertions.assertEquals(1, kafkaActor.consume(10000, "user-created-dlt").size());

        // and no user is saved in the database
        Assertions.assertEquals(0, userRepository.count());
    }
}

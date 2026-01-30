package org.kafka.template.kafka;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.kafka.template.base.BaseLogTest;
import org.kafka.template.creators.UserCreator;
import org.kafka.template.entity.UserEntity;
import org.kafka.template.mapper.UserMapper;
import org.kafka.template.models.User;
import org.kafka.template.repository.UserRepository;
import org.kafka.template.utils.ValidatorUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import jakarta.validation.ConstraintViolationException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserConsumerTest extends BaseLogTest {

    @Mock
    private ValidatorUtils validatorUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Acknowledgment acknowledgment;

    private UserConsumer userConsumer;

    @BeforeEach
    void setUp() {
        userConsumer = new UserConsumer(validatorUtils, userRepository, userMapper);
        setUpLogger(UserConsumer.class);
    }

    @AfterEach
    void tearDown() {
        tearDownLogger();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {18})
    void consume_ValidAdultUser_ShouldProcessSuccessfully(Integer age) {
        // Given
        User user = UserCreator.createRandomUser();
        user.setAge(age);

        UserEntity userEntity = new UserEntity();
        userEntity.setAge(age);
        userEntity.setId(1L);
        when(userMapper.toUserEntity(any(User.class))).thenReturn(userEntity);

        ConsumerRecord<String, Object> record = createConsumerRecord("user-key", user, 0, 100L);

        doNothing().when(validatorUtils).validate(any(User.class));

        // When
        userConsumer.consume(record, acknowledgment);

        // Then
        verify(validatorUtils).validate(any(User.class));
        verify(acknowledgment).acknowledge();

        // And assert logs
        assertLog(Level.INFO, "Received record: " + record.value());
        assertLog(Level.INFO, "Consumed valid user: " + user + ". partition: 0, offset: 100, key: user-key");
    }

    @Test
    void consume_ValidUnderageUser_ShouldLogWarning() {
        // Given
        User user = UserCreator.createRandomUser();
        user.setAge(14); // Underage user

        UserEntity userEntity = new UserEntity();
        userEntity.setAge(14);
        userEntity.setId(1L);
        when(userMapper.toUserEntity(any(User.class))).thenReturn(userEntity);

        ConsumerRecord<String, Object> record = createConsumerRecord("user-key", user, 1, 200L);

        doNothing().when(validatorUtils).validate(any(User.class));

        // When
        userConsumer.consume(record, acknowledgment);

        // Then
        verify(validatorUtils).validate(any(User.class));
        verify(acknowledgment).acknowledge();

        assertLog(Level.INFO, "Received record: " + record.value());
        assertLog(Level.INFO, "Consumed valid user: " + user + ". partition: 1, offset: 200, key: user-key");
        assertLog(Level.WARN, "Underage user detected");
    }

    @Test
    void consume_InvalidUser_ShouldLogErrorAndRethrowException() {
        // Given
        User user = UserCreator.createRandomUser();
        user.setEmail("invalid-email-format"); // Invalid email format

        ConsumerRecord<String, Object> record = createConsumerRecord("user-key", user, 0, 400L);

        ConstraintViolationException exception = new ConstraintViolationException("Invalid email format", null);
        doThrow(exception).when(validatorUtils).validate(any(User.class));

        // When & Then
        ConstraintViolationException thrown = assertThrows(
                ConstraintViolationException.class,
                () -> userConsumer.consume(record, acknowledgment)
        );

        assertEquals("Invalid email format", thrown.getMessage());
        verify(acknowledgment).acknowledge();

        assertLog(Level.INFO, "Received record: " + record.value());
        assertLog(Level.ERROR, "Invalid user payload received");
    }

    @Test
    void consume_JsonProcessingError_ShouldHandleGracefully() {
        // Given
        ConsumerRecord<String, Object> record = createConsumerRecord("user-key", "invalid-json", 0, 600L);

        // When & Then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> userConsumer.consume(record, acknowledgment)
        );

        assertEquals("Error processing user message", thrown.getMessage());
        verify(acknowledgment).acknowledge();

        assertLog(Level.INFO, "Received record");
        assertLog(Level.ERROR, "Error during message validation or processing");
    }

    private ConsumerRecord<String, Object> createConsumerRecord(String key, Object value, int partition, long offset) {
        return new ConsumerRecord<>("user-created", partition, offset, key, value);
    }
}

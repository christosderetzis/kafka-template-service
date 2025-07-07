package org.kafka.template.kafkatemplateservice.kafka;

import ch.qos.logback.classic.Level;
import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.kafka.template.kafkatemplateservice.models.User;
import org.kafka.template.kafkatemplateservice.utils.ValidatorUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.Acknowledgment;

import jakarta.validation.ConstraintViolationException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserConsumerTest {

    @Mock
    private ValidatorUtils validatorUtils;

    @Mock
    private Acknowledgment acknowledgment;

    private UserConsumer userConsumer;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        userConsumer = new UserConsumer(validatorUtils);

        // Setup log appender
        logger = (Logger) LoggerFactory.getLogger(UserConsumer.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {18})
    void consume_ValidAdultUser_ShouldProcessSuccessfully(Integer age) {
        // Given
        User user = createUser("John Doe", age, "john@example.com");
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
        User user = createUser("Jane Doe", 14, "jane@example.com");
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
        User user = createUser("Invalid User", 25, "invalid-email");
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

    @Test
    void consume_EdgeCaseAge17_ShouldLogWarning() {
        // Given
        User user = createUser("Minor User", 17, "minor@example.com");
        ConsumerRecord<String, Object> record = createConsumerRecord("user-key", user, 0, 800L);

        doNothing().when(validatorUtils).validate(any(User.class));

        // When
        userConsumer.consume(record, acknowledgment);

        // Then
        verify(validatorUtils).validate(any(User.class));
        verify(acknowledgment).acknowledge();

        assertLog(Level.INFO, "Received record");
        assertLog(Level.INFO, "Consumed valid user");
        assertLog(Level.WARN, "Underage user detected");
    }

    private User createUser(String name, Integer age, String email) {
        User user = new User();
        user.setId(1);
        user.setName(name);
        user.setAge(age);
        user.setEmail(email);
        return user;
    }

    private ConsumerRecord<String, Object> createConsumerRecord(String key, Object value, int partition, long offset) {
        return new ConsumerRecord<>("user-created", partition, offset, key, value);
    }

    private void assertLog(Level level, String message) {
        List<ILoggingEvent> logsList = listAppender.list;
        boolean found = logsList.stream()
                .anyMatch(event -> event.getLevel().equals(level) &&
                        event.getFormattedMessage().contains(message));
        assertTrue(found, "Expected log with level " + level + " and message containing: " + message);
    }
}

package org.kafka.template.kafkatemplateservice.kafka;

import org.junit.jupiter.api.AfterEach;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kafka.template.kafkatemplateservice.base.BaseLogTest;
import org.kafka.template.kafkatemplateservice.models.User;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProducerTest extends BaseLogTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, Object>> completableFuture;

    private UserProducer userProducer;
    private User testUser;

    @BeforeEach
    void setUp() {
        userProducer = new UserProducer(kafkaTemplate);
        // Set the topic value using reflection (simulating @Value injection)
        ReflectionTestUtils.setField(userProducer, "userCreatedTopic", "user-created-topic");

        testUser = User.builder()
                .id(1)
                .name("John Doe")
                .email("john@example.com")
                .age(25)
                .build();

        setUpLogger(UserProducer.class);
    }

    @AfterEach
    void tearDown() {
        tearDownLogger();
    }

    @Test
    void sendUser_Success() {
        // Given
        when(kafkaTemplate.send(anyString(), any(User.class))).thenReturn(completableFuture);

        // When
        assertDoesNotThrow(() -> userProducer.sendUser(testUser));

        // Then
        verify(kafkaTemplate).send("user-created-topic", testUser);
        verify(completableFuture).whenComplete(any());
        assertLog(Level.INFO, "Sent user: " + testUser);
    }

    @Test
    void sendUser_CallbackHandlesSuccess() {
        // Given
        when(kafkaTemplate.send(anyString(), any(User.class))).thenReturn(completableFuture);

        // Simulate successful callback
        doAnswer(invocation -> {
            BiConsumer<SendResult<String, Object>, Throwable> callback = invocation.getArgument(0);
            callback.accept(mock(SendResult.class), null); // null exception = success
            return null;
        }).when(completableFuture).whenComplete(any());

        // When
        assertDoesNotThrow(() -> userProducer.sendUser(testUser));

        // Then
        verify(kafkaTemplate).send("user-created-topic", testUser);
        assertLog(Level.INFO, "Sent user: " + testUser);
        assertLog(Level.INFO, "User sent successfully: " + testUser);
    }

    @Test
    void sendUser_CallbackHandlesFailure() {
        // Given
        when(kafkaTemplate.send(anyString(), any(User.class))).thenReturn(completableFuture);

        // Simulate failure callback
        doAnswer(invocation -> {
            BiConsumer<SendResult<String, Object>, Throwable> callback = invocation.getArgument(0);
            callback.accept(null, new RuntimeException("Kafka send failed"));
            return null;
        }).when(completableFuture).whenComplete(any());

        // When
        assertDoesNotThrow(() -> userProducer.sendUser(testUser));

        // Then
        verify(kafkaTemplate).send("user-created-topic", testUser);
        assertLog(Level.INFO, "Sent user: " + testUser);
        assertLog(Level.ERROR, "Failed to send user: Kafka send failed");
    }

    @Test
    void sendUser_SerializationException() {
        // Given
        when(kafkaTemplate.send(anyString(), any(User.class)))
                .thenThrow(new SerializationException("Schema validation failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userProducer.sendUser(testUser);
        });

        assertEquals("Schema validation failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof SerializationException);
        verify(kafkaTemplate).send("user-created-topic", testUser);
        assertLog(Level.ERROR, "Schema validation failed");
    }

    @Test
    void sendUser_WithNullUser() {
        // Given
        when(kafkaTemplate.send(anyString(), isNull())).thenReturn(completableFuture);

        // When
        assertDoesNotThrow(() -> userProducer.sendUser(null));

        // Then
        verify(kafkaTemplate).send("user-created-topic", null);
        verify(completableFuture).whenComplete(any());
        assertLog(Level.INFO, "Sent user: null");
    }

    @Test
    void sendUser_OtherRuntimeException() {
        // Given
        when(kafkaTemplate.send(anyString(), any(User.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userProducer.sendUser(testUser);
        });

        assertEquals("Unexpected error", exception.getMessage());
        verify(kafkaTemplate).send("user-created-topic", testUser);
        // This will not produce the schema validation log, just the exception
    }
}



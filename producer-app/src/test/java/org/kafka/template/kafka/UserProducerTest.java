package org.kafka.template.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kafka.template.base.*;
import org.kafka.template.models.User;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProducerTest extends BaseLogTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, Object>> completableFuture;

    @Mock
    private SendResult<String, Object> sendResult;

    @Mock
    private ProducerRecord<String, Object> producerRecord;

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
        when(kafkaTemplate.send(anyString(), anyString(), any(User.class))).thenReturn(completableFuture);

        // When
        assertDoesNotThrow(() -> userProducer.sendUser(testUser));

        // Then
        verify(kafkaTemplate).send(eq("user-created-topic"), anyString(), eq(testUser));
        verify(completableFuture).whenComplete(any());
        assertLog(Level.INFO, "Sent user: " + testUser);
    }

    @Test
    void sendUser_CallbackHandlesSuccess() {
        // Given
        String testKey = "test-uuid-key";
        when(kafkaTemplate.send(anyString(), anyString(), any(User.class))).thenReturn(completableFuture);
        when(sendResult.getProducerRecord()).thenReturn(producerRecord);
        when(producerRecord.key()).thenReturn(testKey);
        when(producerRecord.value()).thenReturn(testUser);

        // Simulate successful callback
        doAnswer(invocation -> {
            BiConsumer<SendResult<String, Object>, Throwable> callback = invocation.getArgument(0);
            callback.accept(sendResult, null); // null exception = success
            return null;
        }).when(completableFuture).whenComplete(any());

        // When
        assertDoesNotThrow(() -> userProducer.sendUser(testUser));

        // Then
        verify(kafkaTemplate).send(eq("user-created-topic"), anyString(), eq(testUser));
        assertLog(Level.INFO, "Sent user: " + testUser);
        assertLog(Level.INFO, "User sent successfully with key: " + testKey + " and value: " + testUser);
    }

    @Test
    void sendUser_CallbackHandlesFailure() {
        // Given
        when(kafkaTemplate.send(anyString(), anyString(), any(User.class))).thenReturn(completableFuture);

        // Simulate failure callback
        doAnswer(invocation -> {
            BiConsumer<SendResult<String, Object>, Throwable> callback = invocation.getArgument(0);
            callback.accept(null, new RuntimeException("Kafka send failed"));
            return null;
        }).when(completableFuture).whenComplete(any());

        // When
        assertDoesNotThrow(() -> userProducer.sendUser(testUser));

        // Then
        verify(kafkaTemplate).send(eq("user-created-topic"), anyString(), eq(testUser));
        assertLog(Level.INFO, "Sent user: " + testUser);
        assertLog(Level.ERROR, "Failed to send user: Kafka send failed");
    }

    @Test
    void sendUser_SerializationException() {
        // Given
        when(kafkaTemplate.send(anyString(), anyString(), any(User.class)))
                .thenThrow(new SerializationException("Schema validation failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userProducer.sendUser(testUser);
        });

        assertEquals("Schema validation failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof SerializationException);
        verify(kafkaTemplate).send(eq("user-created-topic"), anyString(), eq(testUser));
        assertLog(Level.ERROR, "Schema validation failed: Schema validation failed");
    }

    @Test
    void sendUser_WithNullUser() {
        // Given
        when(kafkaTemplate.send(anyString(), anyString(), isNull())).thenReturn(completableFuture);

        // When
        assertDoesNotThrow(() -> userProducer.sendUser(null));

        // Then
        verify(kafkaTemplate).send(eq("user-created-topic"), anyString(), isNull());
        verify(completableFuture).whenComplete(any());
        assertLog(Level.INFO, "Sent user: null");
    }

    @Test
    void sendUser_OtherRuntimeException() {
        // Given
        when(kafkaTemplate.send(anyString(), anyString(), any(User.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userProducer.sendUser(testUser);
        });

        assertEquals("Unexpected error", exception.getMessage());
        verify(kafkaTemplate).send(eq("user-created-topic"), anyString(), eq(testUser));
        // This will not produce the schema validation log, just the exception
    }

    @Test
    void sendUser_VerifyUUIDKeyGeneration() {
        // Given
        when(kafkaTemplate.send(anyString(), anyString(), any(User.class))).thenReturn(completableFuture);

        // When
        assertDoesNotThrow(() -> userProducer.sendUser(testUser));

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("user-created-topic"), keyCaptor.capture(), eq(testUser));

        String capturedKey = keyCaptor.getValue();
        assertNotNull(capturedKey);
        // Verify it's a valid UUID format (basic regex check)
        assertTrue(capturedKey.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }
}
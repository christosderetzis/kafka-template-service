package org.kafka.template.kafkatemplateservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.kafka.template.kafkatemplateservice.models.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topics.user-created}")
    private String userCreatedTopic;

    public UserProducer(@Qualifier("jsonKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUser(User user) {
        try {
            kafkaTemplate.send(userCreatedTopic, user).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send user: {}", ex.getMessage());
                } else {
                    log.info("User sent successfully: {}", user);
                }
            });
            log.info("Sent user: {}", user);
        } catch (SerializationException e) {
            log.error("Schema validation failed: {}", e.getMessage());
            throw new RuntimeException("Schema validation failed", e);
        }
    }
}

package org.kafka.template.kafkatemplateservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.kafka.template.kafkatemplateservice.models.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "user-created";

    public UserProducer(@Qualifier("jsonKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUser(User user) {
        try {
            kafkaTemplate.send(TOPIC, user).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send user: {}", ex.getMessage());
                } else {
                    log.info("User sent successfully: {}", user);
                }
            });
            log.info("Sent user: {}", user);
        } catch (Exception e) {
            log.error("Failed to send user: {}", e.getMessage());
            throw new RuntimeException("Schema validation failed", e);
        }
    }
}

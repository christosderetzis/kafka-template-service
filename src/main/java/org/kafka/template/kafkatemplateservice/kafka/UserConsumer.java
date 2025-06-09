package org.kafka.template.kafkatemplateservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.kafka.template.kafkatemplateservice.models.User;
import org.kafka.template.kafkatemplateservice.utils.ValidatorUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserConsumer {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ValidatorUtils validatorUtils;

    public UserConsumer(ValidatorUtils validatorUtils) {
        this.validatorUtils = validatorUtils;
    }

    @Observed
    @KafkaListener(topics = "${spring.kafka.topics.user-created}", groupId = "user-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {

            User user = mapper.readValue(record.value(), User.class);
            validatorUtils.validate(user);

            log.info("Consumed valid user: {}. partition: {}, offset: {}, key: {}", user, record.partition(), record.offset(), record.key());

            if (user.getAge() != null && user.getAge() < 18) {
                log.warn("Underage user detected: {}", user);
            }
            ack.acknowledge();

        } catch (ConstraintViolationException e) {
            log.warn("Invalid user payload received: {}", e.getMessage());
            ack.acknowledge();
            throw e; // Re-throw to trigger error handling if necessary
        } catch (Exception e) {
            log.error("Error during message validation or processing", e);
            ack.acknowledge();
            throw new RuntimeException("Error processing user message", e);
        }
    }
}

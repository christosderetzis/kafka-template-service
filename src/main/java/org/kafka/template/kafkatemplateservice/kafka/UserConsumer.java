package org.kafka.template.kafkatemplateservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.kafka.template.kafkatemplateservice.models.User;
import org.kafka.template.kafkatemplateservice.utils.ValidatorUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolationException;

@Service
@Slf4j
public class UserConsumer {

    private final ValidatorUtils validator = new ValidatorUtils();
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = "user-created", groupId = "user-group")
    public void consume(User user) {
        try {

            ValidatorUtils.validateSchema(user);

            log.info("Consumed valid user: {}", user);

            if (user.getAge() != null && user.getAge() < 18) {
                log.warn("Underage user detected: {}", user);
            }

        } catch (ConstraintViolationException e) {
            log.warn("Invalid user payload received: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error during message validation or processing", e);
        }
    }
}

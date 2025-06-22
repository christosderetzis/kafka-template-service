package org.kafka.template.kafkatemplateservice;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.kafka.template.kafkatemplateservice.base.BaseKafkaFunctionalSpec;
import org.kafka.template.kafkatemplateservice.models.User;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

class KafkaTemplateServiceApplicationTests extends BaseKafkaFunctionalSpec {

    @Test
    void contextLoads() throws Exception {

        //given:
        User user = User.builder()
                .id(1)
                .name("John Doe")
                .email("john.doe@gmail.com")
                .age(30)
                .build();

        //when:
        kafkaActor.produce("1", user);

        //then assertLog
        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> assertLog(Level.INFO, "Consumed valid user: User(id=1, name=John Doe, email=john.doe@gmail.com, age=30)"));
    }

}

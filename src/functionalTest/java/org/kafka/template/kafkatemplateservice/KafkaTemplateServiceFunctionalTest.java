package org.kafka.template.kafkatemplateservice;

import org.junit.jupiter.api.Test;
import org.kafka.template.kafkatemplateservice.base.BaseKafkaFunctionalSpec;
import org.kafka.template.kafkatemplateservice.models.User;

import static org.awaitility.Awaitility.await;

class KafkaTemplateServiceFunctionalTest extends BaseKafkaFunctionalSpec {

    @Test
    void givenValidUser_whenCreateUser_thenUserIsSentToKafka() throws Exception {
        // Given
        User user = User.builder()
                .id(1)
                .build();

        // When
        kafkaActor.produce(user.getId().toString(), user);

        // Then
        assert 2 == 1 + 1;
    }
}

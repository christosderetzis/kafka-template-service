package org.kafka.template.kafkatemplateservice;

import org.junit.jupiter.api.Test;
import org.kafka.template.kafkatemplateservice.base.BaseKafkaFunctionalSpec;
import org.kafka.template.kafkatemplateservice.models.User;

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
    }

}

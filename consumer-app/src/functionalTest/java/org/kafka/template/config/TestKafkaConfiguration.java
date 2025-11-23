package org.kafka.template.config;

import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.kafka.template.actors.KafkaActor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Properties;

@Configuration
public class TestKafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.schema-registry-url}")
    private String registryUrl;

    @Bean
    public Properties jsonProducerProps() {
        Properties jsonProducerProps = new Properties();
        jsonProducerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        jsonProducerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        jsonProducerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class);
        jsonProducerProps.put(KafkaJsonSchemaSerializerConfig.LATEST_COMPATIBILITY_STRICT, "false");
        jsonProducerProps.put(KafkaJsonSchemaSerializerConfig.FAIL_INVALID_SCHEMA, "false");
        jsonProducerProps.put(KafkaJsonSchemaSerializerConfig.USE_LATEST_VERSION, "true");
        jsonProducerProps.put(KafkaJsonSchemaSerializerConfig.AUTO_REGISTER_SCHEMAS, "false");
        jsonProducerProps.put(ProducerConfig.CLIENT_ID_CONFIG, "user-group");
        jsonProducerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        jsonProducerProps.put(KafkaJsonSchemaSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        return jsonProducerProps;
    }

    @Bean
    public Properties jsonconsumerProps() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "user-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaJsonSchemaDeserializer.class);
        consumerProps.put(KafkaJsonSchemaSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return consumerProps;
    }

    @Bean
    public Properties errorConsumerProps() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "func-test-error");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaJsonSchemaDeserializer.class);
        consumerProps.put(KafkaJsonSchemaSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return consumerProps;
    }

    @Bean
    @Lazy
    public KafkaActor kafkaActor(
            @Value("${spring.kafka.topics.user-created}") String userCreatedTopicName) {
        return new KafkaActor(
                bootstrapServers,
                jsonProducerProps(),
                jsonconsumerProps(),
                errorConsumerProps(),
                userCreatedTopicName
        );
    }
}

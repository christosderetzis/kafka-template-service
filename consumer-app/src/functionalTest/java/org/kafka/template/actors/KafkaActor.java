package org.kafka.template.actors;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class KafkaActor {

    private static KafkaAdmin kafkaAdmin;
    private static KafkaConsumer<String, String> jsonConsumer;
    private static KafkaConsumer<String, String> errorConsumer;
    private static KafkaProducer<String, Object> producer;
    private final List<String> topics = new ArrayList<>();

    public KafkaActor(String bootstrapServers, Properties producerProps, Properties jsonConsumerProps, Properties errorConsumerProps, String topic) {
        log.info("KafkaActor initialized with KafkaAdmin, Consumer, and Producer");
        topics.addAll(List.of("user-created", "user-created-dlt"));

        if (kafkaAdmin == null) {
            kafkaAdmin = new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
        }

        for (String t : topics) {
            kafkaAdmin.createOrModifyTopics(new NewTopic(t, 1, (short) 1));
            log.info("Created or modified topic: {}", t);
        }

        producer = new KafkaProducer<String, Object>(producerProps);
        jsonConsumer = new KafkaConsumer<String, String>(jsonConsumerProps);
        errorConsumer = new KafkaConsumer<String, String>(errorConsumerProps);
    }

    public RecordMetadata produce(String key, Object value, String topic) throws Exception {
        RecordMetadata recordMetadata = producer.send(new ProducerRecord<>(topic, key, value)).get();
        log.info("Produced message to topic: {}, partition: {}, offset: {}",
                 recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
        producer.flush();
        return recordMetadata;
    }

    public List<ConsumerRecord<String, String>> consume(int maxRecords, String topic) {
        KafkaConsumer<String, String> consumer = topic.endsWith("-dlt") ? errorConsumer : jsonConsumer;
        consumer.subscribe(List.of(topic));
        ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofMillis(1000));
        List<ConsumerRecord<String, String>> recordList = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {
            if (recordList.size() < maxRecords) {
                recordList.add(record);
                log.info("Consumed message from topic: {}, partition: {}, offset: {}, key: {}, value: {}",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value());
            } else {
                break;
            }
        }
        consumer.commitSync();
        log.info("Committed offsets for consumed messages from topic: {}", topic);
        return recordList;
    }

    public void close() {
        if (producer != null) {
            producer.close();
            log.info("KafkaProducer closed");
        }
        if (jsonConsumer != null) {
            jsonConsumer.close();
            log.info("KafkaConsumer closed");
        }
    }
}

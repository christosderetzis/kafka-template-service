package org.kafka.template.kafkatemplateservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseKafkaFunctionalSpec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final Network network = Network.newNetwork();

    private static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.9.1").asCompatibleSubstituteFor("apache/kafka"))
            .withNetwork(network);

    private static final GenericContainer<?> schemaRegistry =
            new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.9.1"))
                    .withNetwork(network)
                    .withExposedPorts(8081)
                    .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                    .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                            "PLAINTEXT://" + kafkaContainer.getNetworkAliases().get(0) + ":9092")
                    .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.schema-registry-url", () -> "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getFirstMappedPort());
        registry.add("spring.kafka.topics.user-created", () -> "user-created");

        // Register the ObjectMapper as a bean if needed
        registry.add("objectMapper", () -> OBJECT_MAPPER);
    }

    @BeforeAll
    static void setupContainer() {
        kafkaContainer.start();
        schemaRegistry.start();
    }

    @AfterAll
    static void cleanupContainer() {
        if (kafkaContainer.isRunning()) {
            kafkaContainer.stop();
        }
        if (schemaRegistry.isRunning()) {
            schemaRegistry.stop();
        }
        network.close();
    }
}

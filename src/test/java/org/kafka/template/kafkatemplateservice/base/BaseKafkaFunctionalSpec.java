package org.kafka.template.kafkatemplateservice.base;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.kafka.template.kafkatemplateservice.actors.KafkaActor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class BaseKafkaFunctionalSpec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private ListAppender<ILoggingEvent> appender;

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

    @Autowired
    protected KafkaActor kafkaActor;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.schema-registry-url", () -> "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getFirstMappedPort());

        // Register the ObjectMapper as a bean if needed
        registry.add("objectMapper", () -> OBJECT_MAPPER);
    }

    private void startLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(appender);
        appender.start();
    }

    protected boolean assertLog(Level level, String message) {
        return appender.list.stream().anyMatch(event -> event.getLevel().equals(level) && event.getFormattedMessage().contains(message));
    }

    protected void assertLogCount(Level level, String message, Integer count) {
        long actualCount = appender.list.stream()
                .filter(event -> event.getFormattedMessage().contains(message) && event.getLevel().equals(level))
                .count();
        assert actualCount == count;
    }

    @BeforeEach
    void setup() {
        startLogAppender();
        appender.list.clear();
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

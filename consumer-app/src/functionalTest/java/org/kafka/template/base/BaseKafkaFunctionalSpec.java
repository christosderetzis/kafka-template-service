package org.kafka.template.base;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.kafka.template.actors.KafkaActor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT30S")
@Slf4j
public class BaseKafkaFunctionalSpec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private ListAppender<ILoggingEvent> appender;

    private static final Network network = Network.newNetwork();

    private static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.9.1").asCompatibleSubstituteFor("apache/kafka"))
            .withNetworkAliases("kafka")
            .withNetwork(network);

    private static final GenericContainer<?> schemaRegistry =
            new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.9.1"))
                    .withNetworkAliases("schema_registry")
                    .withNetwork(network)
                    .withExposedPorts(8081)
                    .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                    .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                            "PLAINTEXT://kafka:9092")
                    .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    @Autowired
    protected KafkaActor kafkaActor;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.schema-registry-url", () -> "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getFirstMappedPort());

        // Disable SASL for tests (TestContainers Kafka uses PLAINTEXT)
        registry.add("spring.kafka.security.protocol", () -> "PLAINTEXT");
        registry.add("spring.kafka.sasl.mechanism", () -> "");
        registry.add("spring.kafka.sasl.jaas.username", () -> "");
        registry.add("spring.kafka.sasl.jaas.password", () -> "");

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

    protected static void insertSchemaToCluster(String topicName, String schemaResource) throws IOException, InterruptedException, JSONException {
        String rawSchema = readFromFileToString(schemaResource);
        String schemaDescription = new JSONObject()
                .put("schemaType", "JSON")
                .put("schema", rawSchema)
                .toString();

        String[] createSchemaCommand = {
                "curl", "-X", "POST",
                "-H", "Content-Type: application/vnd.schemaregistry.v1+json",
                "--data", schemaDescription,
                "http://localhost:8081/subjects/" + topicName + "-value/versions"
        };

        Container.ExecResult result = schemaRegistry.execInContainer(createSchemaCommand);

        Pattern regex = Pattern.compile("\\{\"id\":\\d+\\}");
        Matcher matcher = regex.matcher(result.getStdout());
        if (!matcher.find()) {
            throw new AssertionError("Schema registration response did not match expected pattern");
        }
    }

    private static String readFromFileToString(String classpathResource) throws IOException {
        try (InputStream inputStream = new ClassPathResource(classpathResource).getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    @BeforeEach
    void setup() {
        startLogAppender();
        appender.list.clear();
    }

    @BeforeAll
     static void setupContainer() throws IOException, InterruptedException, JSONException {
        kafkaContainer.start();
        schemaRegistry.start();
        insertSchemaToCluster("user-created", "schemas/user-schema.json");
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

# Kafka Template Service

This is a Spring Boot multi-module application that demonstrates the use of Kafka for asynchronous communication. The application provides a RESTful API for creating users, which are then published to a Kafka topic. A Kafka consumer listens to this topic and processes the user information.

## Project Structure

The project is organized as a multi-module application:

* **common-module**: Shared DTOs, models, and utilities used by both producer and consumer
* **producer-app**: Spring Boot application that exposes REST API and produces messages to Kafka (runs on port 9091)
* **consumer-app**: Spring Boot application that consumes messages from Kafka (runs on port 9090)

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

* Java 21
* Docker (for running Kafka and related services)

**Note:** Gradle wrapper is included in the project, so you don't need to install Gradle separately.

### Building the project

To build the entire project (all modules):

```bash
./gradlew build
```

To build a specific module:

```bash
./gradlew :producer-app:build
./gradlew :consumer-app:build
./gradlew :common-module:build
```

To clean and build:

```bash
./gradlew clean build
```

To refresh dependencies:

```bash
./gradlew build --refresh-dependencies
```

## Running the Infrastructure

Before running the application modules, start the required services (Kafka, Zookeeper, Schema Registry):

```bash
docker-compose up -d
```

### Extended Docker Compose

For a more complete environment, including Kafka Connect, ksqlDB, and the Confluent Control Center, you can use the extended Docker Compose file:

```bash
docker-compose -f docker-compose-confluent.yml up -d
```

### Uploading the schema

To upload the user schema to the Schema Registry, you can use the following command:

```bash
curl -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data "{\"schemaType\":\"JSON\", \"schema\":\"$(tr -d '\n\r' < common-module/src/main/resources/schemas/user-schema.json | sed 's/"/\\"/g')\"}" \
  http://localhost:8081/subjects/user-created-value/versions

```

## Running the Modules

After starting the infrastructure with Docker Compose, you can run each module independently.

### Running the Producer Application

The producer application exposes a REST API on port 9091:

```bash
./gradlew :producer-app:bootRun
```

The application will be available at `http://localhost:9091`

### Running the Consumer Application

The consumer application listens to Kafka topics on port 9090:

```bash
./gradlew :consumer-app:bootRun
```

The application will be available at `http://localhost:9090`

### Running Both Applications

To run both applications simultaneously, open two terminal windows and run each command in a separate terminal.

## API

The application exposes the following RESTful endpoint:

* `POST /users`: Creates a new user.

**Request body:**

```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "age": 30
}
```

**Response:**

```
User created successfully
```

## Kafka Integration

The application uses Kafka for asynchronous communication. When a new user is created via the API, a message is sent to the `user-created` topic. A Kafka consumer listens to this topic and processes the user information.

### User Producer

The `UserProducer` class is responsible for sending user information to the `user-created` topic.

### User Consumer

The `UserConsumer` class is responsible for consuming user information from the `user-created` topic. It validates the user data and logs a warning if the user is underage.

## Testing

### Running Unit Tests

Run all unit tests across all modules:

```bash
./gradlew test
```

**Note:** Functional tests are located in `src/functionalTest/java` and are automatically excluded from regular test runs.

### Running Tests for Specific Modules

```bash
# Producer module tests
./gradlew :producer-app:test

# Consumer module tests
./gradlew :consumer-app:test

# Common module tests
./gradlew :common-module:test
```

### Running Functional Tests

```bash
# All functional tests
./gradlew functionalTest

# Producer module functional tests
./gradlew :producer-app:functionalTest

# Consumer module functional tests
./gradlew :consumer-app:functionalTest
```

### Test Coverage

The project uses JaCoCo for code coverage. After running tests, you can view the coverage reports at:

- Producer module: `producer-app/build/reports/jacoco/test/html/index.html`
- Consumer module: `consumer-app/build/reports/jacoco/test/html/index.html`

**Note:** Each module generates its own coverage report. JaCoCo is configured for producer-app and consumer-app modules with an 80% line coverage requirement.

Generate coverage reports:
```bash
./gradlew test jacocoTestReport
```

Check coverage verification:
```bash
./gradlew jacocoTestCoverageVerification
```

### Troubleshooting Tests

Gradle automatically handles dependencies between modules, so you typically won't encounter dependency issues. If you do:

1. **Clean and rebuild:**
   ```bash
   ./gradlew clean build
   ```

2. **Refresh dependencies:**
   ```bash
   ./gradlew build --refresh-dependencies
   ```

3. **Run with debug info:**
   ```bash
   ./gradlew test --info
   ```

## Monitoring with Prometheus and Grafana

Both the producer-app and consumer-app expose metrics via Spring Boot Actuator and Micrometer. You can monitor both applications using Prometheus and Grafana.

### Starting Monitoring Stack

To start Prometheus and Grafana:

```bash
docker-compose -f docker-compose-monitoring.yml up -d
```

### Accessing the Monitoring Tools

- **Prometheus UI**: http://localhost:9093
  - Scrapes metrics from consumer-app (port 9090) and producer-app (port 9091)
  - Metrics endpoint: `/actuator/prometheus`

- **Grafana UI**: http://localhost:3000
  - Default credentials: `admin` / `admin`
  - Prometheus is pre-configured as a data source

### Available Metrics

Both applications expose the following metrics:

- **Application Metrics**: JVM memory, threads, garbage collection
- **HTTP Metrics**: Request count, duration, status codes
- **Kafka Metrics**: Producer/consumer metrics, message rates
- **Custom Metrics**: Any application-specific metrics via Micrometer

### Querying Metrics in Prometheus

You can query metrics for each application separately using the `job` label:

```promql
# Consumer app metrics
http_server_requests_seconds_count{job="consumer-app"}

# Producer app metrics
http_server_requests_seconds_count{job="producer-app"}

# All applications
http_server_requests_seconds_count
```

### Port Summary

- **Consumer App**: 9090 - Kafka consumer + actuator endpoints
- **Producer App**: 9091 - REST API + actuator endpoints
- **Prometheus**: 9092 - Metrics collection
- **Grafana**: 3000 - Metrics visualization
- **Kafka**: 9092 (internal) - Message broker
- **Schema Registry**: 8081 - Schema management
- **Zipkin**: 9411 - Distributed tracing

## Dependencies

The project uses the following major dependencies:

* Spring Boot
* Spring Kafka
* Confluent Schema Registry
* Testcontainers
* Lombok
* Micrometer
* Zipkin

# Kafka Template Service

This is a Spring Boot application that demonstrates the use of Kafka for asynchronous communication. The application provides a RESTful API for creating users, which are then published to a Kafka topic. A Kafka consumer listens to this topic and processes the user information.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

* Java 21
* Maven

### Building the project

To build the project, run the following command in the root directory:

```bash
./mvnw clean install
```

## Running the application

To run the application, you can use the following command:

```bash
docker-compose up -d
```
This will start the application along with the required services (Kafka, Zookeeper, Schema Registry).

### Extended Docker Compose

For a more complete environment, including Kafka Connect, ksqlDB, and the Confluent Control Center, you can use the extended Docker Compose file:

```bash
docker-compose -f docker-compose-confluent.yml up -d
```

### Uploading the schema

To upload the user schema to the Schema Registry, you can use the following command:

```bash
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
--data @src/main/resources/schemas/user-schema.json \
http://localhost:8081/subjects/user-created-value/versions
```

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

To run the tests, use the following command:

```bash
./mvnw test
```

To run the functional tests, use the following command:

```bash
./mvnw verify -Pfunctional-tests
```

## Dependencies

The project uses the following major dependencies:

* Spring Boot
* Spring Kafka
* Confluent Schema Registry
* Testcontainers
* Lombok
* Micrometer
* Zipkin

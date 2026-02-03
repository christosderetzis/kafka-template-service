FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.source="https://github.com/christosderetzis/kafka-template-service"
LABEL org.opencontainers.image.description="Kafka Producer Service"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 9091

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9091/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

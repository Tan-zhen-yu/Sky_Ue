FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/traffic-sim-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

RUN apk add --no-cache curl

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "/app/app.jar"]

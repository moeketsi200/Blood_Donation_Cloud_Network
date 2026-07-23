# ─────────────────────────────────────────────
# Stage 1: Build – compile the Spring Boot JAR
# ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom first so dependency layer is cached independently of source changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build (skip tests – run them in CI, not at image build time)
COPY src ./src
RUN mvn package -DskipTests -B

# ─────────────────────────────────────────────
# Stage 2: Runtime – lean JRE-only image
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user (security best practice)
RUN addgroup -S bloodbroker && adduser -S bloodbroker -G bloodbroker
USER bloodbroker

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=builder /app/target/blood-integration-broker-1.0-SNAPSHOT.jar app.jar

# Spring Boot default port
EXPOSE 8080

# JVM: respect container memory limits & use virtual threads (Java 21)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

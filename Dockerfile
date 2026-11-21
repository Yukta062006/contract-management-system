# Multi-stage Dockerfile for Spring Boot 3 + Java 21 LTS
# Stage 1: Build the artifact
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the production JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Stage 2: Lightweight runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create directory for persistent uploads
RUN mkdir -p /app/uploads

# Copy built JAR from builder
COPY --from=build /app/target/contract-management-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
ENV FILE_UPLOAD_DIR=/app/uploads
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]

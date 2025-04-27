# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:23-jdk as builder

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and the pom.xml
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .

# Download dependencies (this layer will be cached)
RUN ./mvnw dependency:go-offline -B

# Copy the project source
COPY src src/

# Package the application
RUN ./mvnw package -DskipTests

# Use a smaller base image for the final stage
FROM eclipse-temurin:23-jre

WORKDIR /app

# Copy the packaged jar file from the builder stage
COPY --from=builder /app/target/reservation-event-processor-*.jar app.jar

EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]

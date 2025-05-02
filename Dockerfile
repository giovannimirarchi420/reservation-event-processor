FROM maven:3.9.9-eclipse-temurin-23-alpine AS build

# Build argument for database type ('postgres' or 'oracle')
ARG DB_TYPE=postgres

WORKDIR /app
COPY . .
RUN ./mvnw clean package -Ppro-${DB_TYPE} -DskipTests

# Use a smaller base image for the final stage
FROM eclipse-temurin:23-jre

# Build argument for database type ('postgres' or 'oracle')
ARG DB_TYPE=postgres

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=pro,${DB_TYPE}

EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]

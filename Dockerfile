FROM maven:3.9.9-eclipse-temurin-23-alpine AS build

WORKDIR /app
COPY . .
RUN ./mvnw clean package -Ppro-oracle -DskipTests

# Use a smaller base image for the final stage
FROM eclipse-temurin:23-jre

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=pro

EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]

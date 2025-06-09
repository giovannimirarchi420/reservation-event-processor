# Reservation Event Processor

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-23-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-blue.svg?logo=kubernetes)](https://kubernetes.io/)

A Spring Boot service responsible for processing resource reservation events (like start and end times) and dispatching notifications via webhooks.

> **Note:** This service is part of a larger project available at [cloud-resource-reservation](https://github.com/giovannimirarchi420/cloud-resource-reservation), which includes the main backend, frontend, Keycloak configuration, and Docker Compose setup to run the complete system.

## 🎯 Role in the Project

The `reservation-event-processor` complements the main [reservation-be](https://github.com/giovannimirarchi420/cloud-resource-reservation/tree/main/reservation-be) service by handling time-based, asynchronous tasks. It periodically checks the database for resource reservations that are about to start or have just ended and triggers external actions (like sending webhook notifications) based on these events.

## ✨ Key Features

*   **Event Processing:** Regularly checks for upcoming and concluded reservation events.
*   **Webhook Notifications:** Sends notifications for `EVENT_START` and `EVENT_END` to configured external services via webhooks. The payload includes batch event information and currently active resources for the user.

For detailed webhook payload examples and documentation, see [webhook-payload-examples.md](../webhook-payload-examples.md) in the root project directory.
*   **Scheduling:** Uses Spring Scheduler for periodic checks.
*   **Persistence:** Interacts with the PostgreSQL database using Spring Data JPA.
*   **Flexible Configuration:** Configurable via environment variables or `application.properties`/`application.yml` files.

## 🛠️ Technology Stack

*   Java 23
*   Spring Boot 3.2.4
*   Spring Data JPA
*   Spring Web (for Actuator)
*   PostgreSQL
*   Maven
*   Lombok
*   Keycloak Admin Client (potentially for fetching user/resource details, though current usage seems limited in the provided code)
*   Docker
*   Kubernetes (configurations provided in `k8s/`)

## 🚀 Getting Started

### Prerequisites

*   JDK 23 or higher
*   Maven 3.6+
*   Accessible PostgreSQL Database
*   (Optional) Docker
*   (Optional) Access to a Kubernetes cluster

### Configuration

The service can be configured using environment variables or an `application.properties`/`application.yml` file. Key configuration options include:

*   `SPRING_PROFILES_ACTIVE`: Active Spring profile (e.g., `dev`, `test`, `pro`).
*   `SPRING_DATASOURCE_URL`: PostgreSQL database URL.
*   `SPRING_DATASOURCE_USERNAME`: Database username.
*   `SPRING_DATASOURCE_PASSWORD`: Database password.
*   `KEYCLOAK_AUTH_SERVER_URL`: Keycloak server URL.
*   `KEYCLOAK_REALM`: Keycloak realm.
*   `KEYCLOAK_CLIENT_ID`: Client ID for service authentication.
*   `KEYCLOAK_CLIENT_SECRET`: Client secret for service authentication.
*   `WEBHOOK_CONFIG_URL`: URL to fetch webhook configuration (if applicable).
*   `EVENT_PROCESSOR_RATE`: Scheduler execution frequency (in milliseconds, e.g., `60000` for 1 minute).
*   `APP_DEFAULT_ZONE_ID`: Default time zone (e.g., `Europe/Rome`).
*   `LOGGING_LEVEL_ROOT`: Global logging level (e.g., `INFO`, `DEBUG`).

For Kubernetes, these configurations are managed via `ConfigMap` and `Secret` in the `k8s/` directory.

### Build

To compile the project and create the executable JAR file, use Maven:

```bash
./mvnw clean package
# or
# mvn clean package
```

This will create a JAR file in the `target/` directory.

### Running

#### Local Execution

```bash
java -jar target/reservation-event-processor-*.jar --spring.profiles.active=dev # or other profiles
```

Ensure you provide the necessary environment variables or an `application-dev.properties` file with the required configurations.

#### Docker

A `Dockerfile` is provided to build a Docker image.

1.  **Build the image:**
    ```bash
    docker build -t reservation-event-processor .
    ```

2.  **Run the container:**
    ```bash
    docker run -e SPRING_PROFILES_ACTIVE=pro \
           -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-postgres-host:5432/resourcemgmt \
           -e SPRING_DATASOURCE_USERNAME=your_user \
           -e SPRING_DATASOURCE_PASSWORD=your_password \
           # ... other environment variables ... \
           reservation-event-processor
    ```

#### Kubernetes

Configurations for Kubernetes deployment are located in the `k8s/` directory. You need to apply the `ConfigMap`, `Secret`, and `Deployment` manifests.

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml # Ensure secrets are Base64 encoded
kubectl apply -f k8s/deployment.yaml
```

## 🔌 API Endpoints

The service primarily exposes Spring Boot Actuator endpoints for monitoring:

*   `/actuator/health`: Checks the application's health status.
*   Other Actuator endpoints might be available depending on the configuration.

## 🤝 Contributing

1.  Fork the repository.
2.  Create a feature branch (`git checkout -b feature/amazing-feature`).
3.  Commit your changes (`git commit -m 'Add some amazing feature'`).
4.  Push to the branch (`git push origin feature/amazing-feature`).
5.  Open a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file in the root project directory for details.

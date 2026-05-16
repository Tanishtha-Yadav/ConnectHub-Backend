# ConnectHub - Notification Service

The `notification-service` is a background-heavy microservice responsible for keeping users informed. It handles in-app notifications and background jobs, ensuring that offline users receive email updates when important events happen.

## 🚀 Features
- **In-App Notifications:** Stores and serves historical notifications (like mentions, room invites, or system alerts).
- **Offline Email Tasks:** Runs scheduled background tasks (`OfflineEmailTask`) to gather unread messages and email them to users who are currently disconnected.
- **Redis Integration:** Subscribes to Redis events to instantly process notification events sent by other microservices.
- **Security:** Ensures only authenticated users can access or clear their personal notifications.

## 🛠️ Tech Stack
- **Java 17** & **Spring Boot 3.x**
- **Spring Data JPA** & **Hibernate**
- **Spring Data Redis** (For Pub/Sub event listening)
- **Spring Mail** (For sending SMTP offline emails)
- **MySQL Database**
- **Docker**

## 📂 Project Structure
- `src/main/java/.../controller`: REST endpoints for users to fetch/clear their notifications (`NotificationController`).
- `src/main/java/.../model`: Core entities like `Notification` and `NotificationType`.
- `src/main/java/.../service`: The `NotificationService` and the background `OfflineEmailTask` scheduler.
- `src/test/`: Unit tests for notification logic and email triggers.

## 🔧 Environment Variables
This service retrieves its configuration via the centralized `ConnectHub-Backend/.env` file. It relies heavily on:
* Database credentials.
* Redis connection strings.
* SMTP Email credentials (for the `OfflineEmailTask`).

## 🐳 Deployment & Running Locally
This service is designed to be run alongside the rest of the ConnectHub microservices using Docker Compose.

**To run via Docker:**
```bash
# Navigate to the root ConnectHub-Backend folder
docker-compose up -d notification-service
```

**To run locally for development (Maven):**
```bash
./mvnw spring-boot:run
```

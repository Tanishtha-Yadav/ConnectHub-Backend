# ConnectHub - Presence Service

The `presence-service` is a highly optimized, lightweight microservice responsible for tracking and serving the real-time online/offline status of users. It works in tandem with the WebSocket handler to know exactly when a user connects or disconnects.

## 🚀 Features
- **Real-Time Status Tracking:** Keeps track of exactly who is online across the entire platform.
- **High Performance:** Utilizes Redis as a fast, in-memory data store to handle constant status updates without hitting a slow relational database.
- **Status Broadcasting:** Allows other microservices (like the Message Service or Notification Service) to instantly check if a user is online before deciding to send an offline email or a live push notification.

## 🛠️ Tech Stack
- **Java 17** & **Spring Boot 3.x**
- **Spring Data Redis** (For blazing-fast in-memory tracking)
- **Docker**

## 📂 Project Structure
- `src/main/java/.../config`: Connects the service to the Redis cluster (`RedisConfig`).
- `src/main/java/.../controller`: Endpoints for other services to query a user's presence (`PresenceController`).
- `src/main/java/.../service`: Core logic for updating and retrieving online statuses (`PresenceService`).
- `src/test/`: Unit testing for presence logic.

## 🔧 Environment Variables
This service retrieves its configuration via the centralized `ConnectHub-Backend/.env` file. It requires standard JWT keys and Redis connection details.

## 🐳 Deployment & Running Locally
This service is designed to be run alongside the rest of the ConnectHub microservices using Docker Compose.

**To run via Docker:**
```bash
# Navigate to the root ConnectHub-Backend folder
docker-compose up -d presence-service
```

**To run locally for development (Maven):**
```bash
./mvnw spring-boot:run
```

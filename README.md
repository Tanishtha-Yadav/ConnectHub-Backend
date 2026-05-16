# ConnectHub - Message Service

The `message-service` is a core microservice of the ConnectHub application responsible for handling real-time chat, message storage, and delivery tracking. It works closely with the WebSocket Handler and Room Service to deliver messages seamlessly to users.

## 🚀 Features
- **Send & Edit Messages:** Users can send new messages and edit existing ones within a chat room.
- **Delivery Status Tracking:** Tracks the lifecycle of a message (`SENT`, `DELIVERED`, `READ`).
- **Rich Message Types:** Supports text, images, and system notifications via `MessageType`.
- **JWT Security Integration:** Ensures that only authorized participants in a room can send or view messages.
- **Admin Management:** Endpoints for platform administrators to monitor or moderate messages.

## 🛠️ Tech Stack
- **Java 17** & **Spring Boot 3.x**
- **Spring Security** (JWT validation)
- **Spring Data JPA** & **Hibernate**
- **MySQL Database**
- **Docker**

## 📂 Project Structure
- `src/main/java/.../controller`: REST API endpoints (`MessageResource`, `AdminController`).
- `src/main/java/.../model`: Core entities (`Message`, `DeliveryStatus`, `MessageType`).
- `src/main/java/.../service`: Business logic for message validation and routing.
- `src/main/java/.../dto`: Data Transfer Objects for clean API requests/responses.
- `src/test/`: Unit tests and mock testing for message service logic.

## 🔧 Environment Variables
This service retrieves its configuration via the centralized `ConnectHub-Backend/.env` file. It requires standard database and JWT keys.

## 🐳 Deployment & Running Locally
This service is designed to be run alongside the rest of the ConnectHub microservices using Docker Compose.

**To run via Docker:**
```bash
# Navigate to the root ConnectHub-Backend folder
docker-compose up -d message-service
```

**To run locally for development (Maven):**
```bash
./mvnw spring-boot:run
```

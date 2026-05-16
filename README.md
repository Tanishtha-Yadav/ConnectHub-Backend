# ConnectHub - WebSocket Handler

The `websocket-handler` is a critical infrastructure microservice in the ConnectHub application. It manages all real-time, persistent WebSocket connections with the frontend clients, enabling instant chat messaging, live presence updates, and real-time notifications.

## 🚀 Features
- **Real-Time Communication:** Handles bidirectional data flow between the frontend and the backend using WebSockets (STOMP).
- **Redis Pub/Sub Integration:** Uses Redis Publisher/Subscriber mechanisms to broadcast messages across multiple instances of the WebSocket handler, ensuring a highly scalable chat architecture.
- **WebSocket Security:** Intercepts connection requests (`WebSocketAuthInterceptor`) to validate JWT tokens before establishing the socket connection.
- **Event Listening:** Listens to socket connect/disconnect events (`WebSocketEventListener`) to help the Presence Service track who is currently online.
- **Message Routing:** Routes `ChatMessage` and `NotificationMessage` payloads to the appropriate subscribed clients.

## 🛠️ Tech Stack
- **Java 17** & **Spring Boot 3.x**
- **Spring WebSocket & STOMP**
- **Spring Data Redis** (For Pub/Sub messaging)
- **Spring Security** (JWT validation)
- **Docker**

## 📂 Project Structure
- `src/main/java/.../config`: Configuration for WebSockets, Redis, and Security Interceptors.
- `src/main/java/.../controller`: Endpoints for incoming WebSocket messages (`ChatController`).
- `src/main/java/.../service`: Redis Pub/Sub logic (`RedisMessagePublisher`, `RedisMessageSubscriber`).
- `src/main/java/.../listener`: Lifecycle hooks for socket connections.
- `src/main/java/.../dto`: Payloads for chat and notifications.

## 🔧 Environment Variables
This service retrieves its configuration via the centralized `ConnectHub-Backend/.env` file. It requires standard JWT keys and Redis connection details.

## 🐳 Deployment & Running Locally
This service is designed to be run alongside the rest of the ConnectHub microservices using Docker Compose.

**To run via Docker:**
```bash
# Navigate to the root ConnectHub-Backend folder
docker-compose up -d websocket-handler
```

**To run locally for development (Maven):**
```bash
./mvnw spring-boot:run
```

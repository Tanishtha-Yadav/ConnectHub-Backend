# ConnectHub - Room Service

The `room-service` is a core microservice of the ConnectHub application responsible for managing chat spaces, channels, and direct message groups. It handles the creation of rooms, membership management, and enforces room-level access control.

## 🚀 Features
- **Room Management:** Endpoints to create, update, and fetch chat rooms.
- **Room Types:** Categorization of spaces (e.g., direct messages, private groups, public channels).
- **Membership & Roles:** Manage users within a room, including assigning specific `MemberRole`s.
- **Access Control:** Ensures users can only access or modify rooms they are authorized to be in.
- **Admin Tools:** Specialized endpoints for platform moderators to oversee room activities.

## 🛠️ Tech Stack
- **Java 17** & **Spring Boot 3.x**
- **Spring Security** (JWT validation)
- **Spring Data JPA** & **Hibernate**
- **MySQL Database**
- **RestTemplate** (For internal microservice communication)
- **Docker**

## 📂 Project Structure
- `src/main/java/.../controller`: REST API endpoints (`RoomController`, `AdminController`).
- `src/main/java/.../model`: Core entities (`Room`, `RoomMember`, `RoomType`, `MemberRole`).
- `src/main/java/.../service`: Business logic for room creation and membership validation.
- `src/main/java/.../dto`: Data Transfer Objects (`CreateRoomRequest`, `AddMemberRequest`).
- `src/test/`: Unit tests and mock testing for room management logic.

## 🔧 Environment Variables
This service retrieves its configuration via the centralized `ConnectHub-Backend/.env` file. It requires standard database and JWT keys.

## 🐳 Deployment & Running Locally
This service is designed to be run alongside the rest of the ConnectHub microservices using Docker Compose.

**To run via Docker:**
```bash
# Navigate to the root ConnectHub-Backend folder
docker-compose up -d room-service
```

**To run locally for development (Maven):**
```bash
./mvnw spring-boot:run
```

# ConnectHub - Auth Service

The `auth-service` is a core microservice of the ConnectHub application responsible for handling user authentication, authorization, registration, and managing user profiles. It secures the platform using JWT (JSON Web Tokens) and integrates with the API Gateway.

## 🚀 Features
- **User Registration & Login:** Standard email/password authentication.
- **OAuth2 Integration:** Support for Google Sign-In.
- **JWT Security:** Stateless, token-based security for all internal microservice communication.
- **Role-Based Access Control:** Differentiates between standard `USER`, `ADMIN`, and `OWNER` roles.
- **Profile Management:** Endpoints for users to update their profile and status.
- **Audit Logging:** Tracks important security events (logins, password changes).

## 🛠️ Tech Stack
- **Java 17** & **Spring Boot 3.x**
- **Spring Security** (with OAuth2 and JWT)
- **Spring Data JPA** & **Hibernate**
- **MySQL Database**
- **Docker**

## 📂 Project Structure
- `src/main/java/.../controller`: REST API endpoints (Auth, OAuth2, Admin).
- `src/main/java/.../security`: JWT generation, parsing, and filters.
- `src/main/java/.../service`: Core business logic.
- `src/test/`: Unit and Integration tests.

## 🔧 Environment Variables
To run this service, you must provide the following environment variables (usually via a `.env` file in the root backend directory injected through Docker Compose):

```env
# Database
MYSQL_ROOT_PASSWORD=your_db_password

# JWT
JWT_SECRET=your_jwt_secret_key
```

## 🐳 Deployment & Running Locally
This service is designed to be run alongside the rest of the ConnectHub microservices using Docker Compose.

**To run via Docker:**
```bash
# Navigate to the root ConnectHub-Backend folder
docker-compose up -d auth-service
```

**To run locally for development (Maven):**
```bash
./mvnw spring-boot:run
```

## 🧪 Testing the API
We have provided tools to make testing easy:
1. **Postman:** Import the `src/test/resources/postman-collection.json` file into Postman.
2. **Terminal:** Run the `src/test/resources/curl-test-commands.sh` script to test endpoints directly from your command line.

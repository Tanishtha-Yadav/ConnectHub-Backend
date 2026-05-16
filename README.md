# ConnectHub - Media Service

The `media-service` is the dedicated file-handling microservice for the ConnectHub application. It is responsible for securely managing file uploads, processing user avatars, chat attachments, and generating secure links using AWS S3.

## 🚀 Features
- **AWS S3 Integration:** Handles all the heavy lifting of communicating with Amazon S3.
- **Secure Uploads:** Generates secure Pre-Signed URLs, allowing the frontend to upload files directly to the S3 bucket without bottlenecking the backend servers.
- **File Retrieval:** Provides secure, temporary access links for viewing attachments and media within chat rooms.
- **CORS Management:** Works alongside the S3 bucket's CORS configuration to ensure smooth cross-origin uploads from the frontend.

## 🛠️ Tech Stack
- **Java 17** & **Spring Boot 3.x**
- **AWS Java SDK (S3)**
- **Spring Security** (JWT validation)
- **Docker**

## 📂 Project Structure
- `src/main/java/.../config`: Contains the `S3Config` connecting to the AWS cloud.
- `src/main/java/.../controller`: REST endpoints for requesting upload/download URLs (`MediaController`).
- `src/main/java/.../service`: Business logic for generating presigned S3 requests.

## 🔧 Environment Variables
This service retrieves its configuration via the centralized `ConnectHub-Backend/.env` file. It relies heavily on AWS credentials. **Never hardcode these values.**

```env
# AWS S3 Configuration
AWS_S3_ACCESS_KEY=your_aws_access_key
AWS_S3_SECRET_KEY=your_aws_secret_key
AWS_S3_BUCKET=your_s3_bucket_name
```

## 🐳 Deployment & Running Locally
This service is designed to be run alongside the rest of the ConnectHub microservices using Docker Compose.

**To run via Docker:**
```bash
# Navigate to the root ConnectHub-Backend folder
docker-compose up -d media-service
```

**To run locally for development (Maven):**
```bash
./mvnw spring-boot:run
```

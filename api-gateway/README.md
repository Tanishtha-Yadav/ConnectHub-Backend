# ConnectHub API Gateway

This module is the Spring Cloud Gateway entry point for ConnectHub. It forwards client requests to the backend services through Eureka service discovery and keeps the service URLs out of the frontend.

## What It Does

- Routes requests for auth, users, and connections to the matching backend service
- Uses Eureka service discovery so service locations can change without updating clients
- Exposes a single gateway port for the frontend to call
- Enables permissive CORS for local development

## Prerequisites

- Java 17 or newer
- A running Eureka server on `http://localhost:8761`
- The backend services registered in Eureka

## Run Locally

From the `api-gateway` folder:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The gateway starts on port `8080`.

## Configuration

The active profile is set to `local` in `src/main/resources/application.properties`.

Main gateway settings live in `src/main/resources/application.yml`:

- `server.port=8080`
- Eureka registry at `http://localhost:8761/eureka/`
- Gateway routes use `lb://` service names

## Route Map

The gateway currently forwards these paths:

- `/api/auth/**` -> `auth-service`
- `/api/users/**` -> `user-service`
- `/api/connections/**` -> `connection-service`

Each route removes the `/api/<segment>/` prefix before forwarding the request to the downstream service.

## Project Layout

```text
api-gateway/
├── src/main/java/com/connecthub/apigateway/
│   ├── ApiGatewayApplication.java
│   └── GatewayConfiguration.java
├── src/main/resources/
│   ├── application.properties
│   └── application.yml
├── src/test/java/com/connecthub/apigateway/
│   └── ApiGatewayApplicationTests.java
├── pom.xml
└── mvnw / mvnw.cmd
```

## Notes

- `GatewayConfiguration` currently allows all origins, methods, and headers for local development.
- If you add a new backend service, add a matching gateway route here and register the service with Eureka.
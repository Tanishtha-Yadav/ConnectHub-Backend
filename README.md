# ConnectHub Backend - API Gateway

This branch README documents the API Gateway module only.

## Purpose

The API Gateway is the single entry point for backend API traffic in ConnectHub.
It routes incoming requests to downstream services using Eureka-based service discovery.

## Key Responsibilities

- Expose a single backend endpoint for clients
- Route API paths to the correct microservice
- Resolve service instances dynamically through Eureka
- Apply development CORS policy at gateway level

## Prerequisites

- Java 17 or newer
- Maven (or Maven Wrapper)
- Running Eureka Server at http://localhost:8761
- Registered downstream services:
	- auth-service
	- user-service
	- connection-service

## Run API Gateway

From the api-gateway folder:

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

Gateway URL: http://localhost:8080

## Current Route Mapping

- /api/auth/** -> auth-service
- /api/users/** -> user-service
- /api/connections/** -> connection-service

Each route rewrites the prefix before forwarding to the target service.

## Configuration Files

- api-gateway/src/main/resources/application.properties
- api-gateway/src/main/resources/application.yml

Important defaults:

- Active profile: local
- Gateway port: 8080
- Eureka default zone: http://localhost:8761/eureka/

## Quick Verification

1. Open Eureka dashboard: http://localhost:8761
2. Verify services are listed as UP
3. Call a gateway endpoint, for example: http://localhost:8080/api/auth/health

## Module Documentation

For full API Gateway implementation details, see:

- api-gateway/README.md

## License

Internal Use Only - ConnectHub Platform

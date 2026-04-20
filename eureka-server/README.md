# ConnectHub Eureka Server

This module runs the Eureka Service Registry for the ConnectHub backend.

## Purpose

- Provides service discovery for backend microservices.
- Exposes Eureka dashboard on port `8761`.
- Keeps this branch focused on Eureka server setup only.

## Tech Stack

- Java 17
- Spring Boot 4.0.5
- Spring Cloud Netflix Eureka Server
- Maven Wrapper

## Configuration

Main configuration file:

- `src/main/resources/application.properties`

Current key settings:

- `spring.application.name=eureka-server`
- `server.port=8761`
- `eureka.client.register-with-eureka=false`
- `eureka.client.fetch-registry=false`

## Run Locally

From this folder (`eureka-server`):

```powershell
.\mvnw.cmd spring-boot:run
```

## Verify

- Dashboard: http://localhost:8761
- Health: http://localhost:8761/actuator/health

Expected health response:

```json
{"status":"UP"}
```

## Build and Test

```powershell
.\mvnw.cmd test
```



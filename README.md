# ConnectHub Backend - Eureka Branch

This branch contains the ConnectHub service discovery and routing layer:

- `eureka-server` runs the Eureka service registry
- `api-gateway` routes client requests to the backend services through Eureka

Use this branch to start the registry first, then the gateway, and then any downstream services that register with Eureka.

## Modules

### `eureka-server`

The Eureka Server provides service discovery for the ConnectHub backend.

- Dashboard: `http://localhost:8761`
- Port: `8761`

Run it from the `eureka-server` folder:

```powershell
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### `api-gateway`

The API Gateway is the single entry point for backend requests.

- Port: `8080`
- Routes requests to `auth-service`, `user-service`, and `connection-service`
- Uses Eureka service discovery so service locations do not need to be hardcoded

Run it from the `api-gateway` folder:

```powershell
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Prerequisites

- Java 17 or newer
- Eureka Server running before any service that depends on discovery
- Backend services registered in Eureka before testing gateway routes

## Recommended Startup Order

1. Start `eureka-server`
2. Start `api-gateway`
3. Start the backend microservices that should register with Eureka

## Quick Checks

### Eureka Server

- Open `http://localhost:8761`
- Confirm the dashboard loads

### API Gateway

- Confirm it starts on port `8080`
- Test a routed endpoint once the downstream service is running, such as `http://localhost:8080/api/auth/**`

## Notes

- This branch is focused on infrastructure for service discovery and routing.
- Individual business endpoints belong in the backend services that register with Eureka.
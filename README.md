# Taskify – Production-Ready Spring Boot Task Management API

Taskify is a professional-grade Task Management REST API built with Spring Boot 3 and Java 21. It is engineered to demonstrate industry-standard backend practices including secure authentication, role-based authorization, observability, and performance optimization.

## Tech Stack

- Java 21
- Spring Boot 3.5.7
- Spring Security 6 (JWT-based)
- Spring Data JPA (Hibernate)
- MySQL 8
- Caffeine (In-memory caching)
- Bucket4j (Rate limiting)
- Spring Boot Actuator & Micrometer (Observability)
- Springdoc OpenAPI 2.8
- JUnit 5 & Mockito
- Docker

## Project Structure

```
src/main/java/com/taskify/taskify
├── config/
│   ├── CacheConfig.java
│   ├── OpenApiConfig.java
│   ├── SecurityBeansConfig.java
│   ├── SecurityConfig.java
│   └── TaskCacheKeyGenerator.java
├── health/
│   └── DatabaseHealthIndicator.java
├── controller/
│   └── v1/
│       ├── AuthController.java
│       └── TaskController.java
├── dto/
│   ├── ApiError.java
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── TaskRequest.java
│   ├── TaskResponse.java
│   └── TokenRefreshRequest.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── RateLimitExceededException.java
│   ├── TaskNotFoundException.java
│   └── TokenException.java
├── model/
│   ├── AuditAction.java
│   ├── AuditLog.java
│   ├── AuditTargetType.java
│   ├── IdempotencyKey.java
│   ├── Priority.java
│   ├── RefreshToken.java
│   ├── Role.java
│   ├── Status.java
│   ├── Task.java
│   └── User.java
├── repository/
│   ├── AuditLogRepository.java
│   ├── IdempotencyKeyRepository.java
│   ├── RefreshTokenRepository.java
│   ├── RoleRepository.java
│   ├── TaskRepository.java
│   ├── TaskSpecification.java
│   └── UserRepository.java
├── security/
│   ├── CorrelationIdFilter.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   ├── RateLimitFilter.java
│   ├── RequestLoggingFilter.java
│   └── SecurityConstants.java
├── service/
│   ├── AuditService.java
│   ├── AuthService.java
│   ├── IdempotencyService.java
│   ├── IdempotencyCleanupTask.java
│   ├── RateLimitService.java
│   ├── RefreshTokenService.java
│   ├── TaskService.java
│   └── impl/
│       ├── AuditServiceImpl.java
│       ├── AuthServiceImpl.java
│       ├── IdempotencyServiceImpl.java
│       ├── RefreshTokenServiceImpl.java
│       ├── TaskServiceImpl.java
│       └── UserDetailsServiceImpl.java
└── TaskifyApplication.java
```

## Authentication & Authorization

### JWT Implementation
- Secure login with short-lived access tokens and long-lived refresh tokens.
- Refresh token rotation and database-backed revocation.
- Secure logout mechanism to invalidate active sessions.

### Authorization Model
- Role-Based Access Control (RBAC): Support for `ROLE_USER` and `ROLE_ADMIN`.
- Ownership Enforcement: Regular users can only access and manage their own tasks.
- Admin Visibility: Administrators have full system-wide visibility, including soft-deleted tasks.

## Security & Reliability

- Rate Limiting: Implemented via Bucket4j with per-user (authenticated) and per-IP (anonymous) limits.
- Soft Deletes: Tasks are soft-deleted to allow for restoration and auditing.
- Encryption: BCrypt password hashing for secure user credential storage.
- Correlation ID: Middleware to track requests across filters and services for debugging.
- Idempotency Persistence: Secure, database-backed storage for critical write operations with configurable expiration (TTL).

## Core Features

- Advanced querying using JPA Specifications
- Unified task search endpoint with filtering, pagination, and sorting
- Asynchronous audit logging of business-critical events (authentication and task lifecycle)
- **Idempotent Write Operations**: Support for `Idempotency-Key` header on task creation to prevent duplicate processing on retries.
- **Focus Mode**: A decision-support endpoint that returns the top 5 most urgent and high-priority tasks for the user.

## Caching & Performance

- Caffeine-based in-memory caching for task reads
- Cache keys scoped by user, role, and query parameters
- Targeted cache invalidation strategy to maintain consistency without global eviction

## Observability

- Structured Logging: Consistent logging with request context (Correlation ID, User ID).
- Audit Logs: Database-backed audit trails for all administrative and authentication actions.
- Health Monitoring: Real-time health checks via Spring Boot Actuator (public `/actuator/health` endpoint).
- Business Metrics: Micrometer-based metrics tracking task operations, rate limiting, and idempotency (available at `/actuator/metrics` for administrators).
- Debug Level Caching: Cache hits and misses monitored without logging sensitive payloads.

## Testing Strategy

- Unit Testing: Business logic coverage for services using Mockito.
- Integration Testing:
    - Web layer testing with MockMvc.
    - JWT and Security flow validation.
    - Caching behavior and invalidation logic.
    - Advanced query and ownership enforcement tests.
    - **Idempotency and Retry Safety tests**.
    - **API Deprecation and Focus Mode integration tests**.

## API Endpoints

### Authentication
- `POST /api/v1/auth/register`: Register a new account.
- `POST /api/v1/auth/login`: Authenticate and receive access/refresh tokens.
- `POST /api/v1/auth/refresh`: Obtain a new access token using a refresh token.
- `POST /api/v1/auth/logout`: Invalidate the current refresh token.

### Tasks
- `GET /api/v1/tasks`: Search, filter, and paginate tasks.
- `GET /api/v1/tasks/focus`: Get the top 5 urgent and prioritized tasks for Focus Mode.
- `GET /api/v1/tasks/{id}`: Retrieve specific task details.
- `POST /api/v1/tasks`: Create a new task. (Supports `Idempotency-Key` header)
- `PUT /api/v1/tasks/{id}`: Update an existing task.
- `DELETE /api/v1/tasks/{id}`: Soft-delete a task.

### Admin
- `POST /api/v1/admin/tasks/{id}/restore`: Restore a soft-deleted task.
- `GET /actuator/health`: Public health check endpoint.
- `GET /actuator/metrics`: System and custom business metrics (Admin only).

## API Versioning

Taskify uses URL-based versioning to ensure backward compatibility as the system evolves.

- **Current Stable Version**: `v1`
- **Base Path**: `/api/v1`

### Handling Changes
- **Non-Breaking Changes**: Added to the current version (e.g., new optional fields, new endpoints).
- **Breaking Changes**: Will trigger a new version (e.g., `v2`), leaving `v1` intact for existing clients.
- **Deprecation**: Older versions will be maintained for a transition period before being retired.

## API Deprecation Policy

To ensure a stable experience for our clients while allowing the API to evolve, we follow a transparent deprecation lifecycle:

1.  **Signaling**: Deprecated endpoints are marked with the `@Deprecated` annotation and the `deprecated` flag in OpenAPI documentation.
2.  **HTTP Headers**: Responses from deprecated endpoints include the following headers:
    *   `Deprecation: true`
    *   `Sunset`: The ISO-8601 date after which the endpoint may be removed.
    *   `Link`: A URL to the successor endpoint or version with `rel="successor-version"`.
3.  **Support Period**: Deprecated endpoints are typically supported for **6 months** before removal.
4.  **Logging**: Usage of deprecated endpoints is monitored via WARN-level logs to identify active clients that need migration.

Clients are encouraged to migrate to successor endpoints as soon as they see the `Deprecation` header.

## Running the Project

### Prerequisites
Ensure the following are installed on your system:
- Java 21
- Maven 3.9+
- Docker

### Database Setup(MySQL via Docker)
Start a MySQL 8 container:
```bash
docker run --name taskify-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=taskify_db \
  -e MYSQL_USER=taskuser \
  -e MYSQL_PASSWORD=taskpass \
  -p 3306:3306 \
  -d mysql:8.0
```
### Application Configuration
Verify the following properties in application.properties:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/taskify_db
spring.datasource.username=taskuser
spring.datasource.password=taskpass

spring.jpa.hibernate.ddl-auto=update

jwt.expiration=900000
jwt.refresh-expiration=604800000
```

### Build and Test
```bash
mvn clean install
mvn test
```

### Run the Application
Start the Spring Boot application:
```bash
mvn spring-boot:run
```

The application will be available at: http://localhost:8080

Swagger UI (API documentation): http://localhost:8080/swagger-ui.html

### Stop the Database
```bash
docker stop taskify-mysql
docker rm taskify-mysql
```
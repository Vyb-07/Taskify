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
├── controller/
│   ├── AuthController.java
│   └── TaskController.java
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
│   ├── Priority.java
│   ├── RefreshToken.java
│   ├── Role.java
│   ├── Status.java
│   ├── Task.java
│   └── User.java
├── repository/
│   ├── AuditLogRepository.java
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
│   ├── RateLimitService.java
│   ├── RefreshTokenService.java
│   ├── TaskService.java
│   └── impl/
│       ├── AuditServiceImpl.java
│       ├── AuthServiceImpl.java
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

## Core Features

- Advanced querying using JPA Specifications
- Unified task search endpoint with filtering, pagination, and sorting
- Asynchronous audit logging of business-critical events (authentication and task lifecycle)

## Caching & Performance

- Caffeine-based in-memory caching for task reads
- Cache keys scoped by user, role, and query parameters
- Targeted cache invalidation strategy to maintain consistency without global eviction

## Observability

- Structured Logging: Consistent logging with request context (Correlation ID, User ID).
- Audit Logs: Database-backed audit trails for all administrative and authentication actions.
- Debug Level Caching: Cache hits and misses monitored without logging sensitive payloads.

## Testing Strategy

- Unit Testing: Business logic coverage for services using Mockito.
- Integration Testing:
    - Web layer testing with MockMvc.
    - JWT and Security flow validation.
    - Caching behavior and invalidation logic.
    - Advanced query and ownership enforcement tests.

## API Endpoints

### Authentication
- `POST /api/auth/register`: Register a new account.
- `POST /api/auth/login`: Authenticate and receive access/refresh tokens.
- `POST /api/auth/refresh`: Obtain a new access token using a refresh token.
- `POST /api/auth/logout`: Invalidate the current refresh token.

### Tasks
- `GET /api/tasks`: Search, filter, and paginate tasks.
- `GET /api/tasks/{id}`: Retrieve specific task details.
- `POST /api/tasks`: Create a new task.
- `PUT /api/tasks/{id}`: Update an existing task.
- `DELETE /api/tasks/{id}`: Soft-delete a task.

### Admin
- `POST /api/admin/tasks/{id}/restore`: Restore a soft-deleted task.

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
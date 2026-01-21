Taskify – Production-Ready Spring Boot Task Management API

Taskify is a production-grade Task Management REST API built with Spring Boot 3 and Java 21, designed to demonstrate how real backend systems are structured, secured, tested, and operated.

This is not a tutorial CRUD project.
It is a deliberately engineered backend that evolves feature-by-feature using industry-grade practices: authentication, authorization, observability, performance optimization, and safety.

🚀 Tech Stack

Java 21

Spring Boot 3

Spring Web

Spring Data JPA (Hibernate)

Spring Security 6

JWT (Access + Refresh Tokens)

MySQL 8 (Dockerized)

OpenAPI / Swagger

Bucket4j (Rate Limiting)

Spring Cache + Caffeine

SLF4J + Logback (Structured Logging)

JUnit 5, Mockito, MockMvc

Docker

🧱 Project Structure
src/main/java/com/taskify/taskify
├── config/
│   ├── CacheConfig.java
│   ├── OpenApiConfig.java
│   ├── SecurityBeansConfig.java
│   ├── SecurityConfig.java
│   └── TaskCacheKeyGenerator.java
│
├── controller/
│   ├── AuthController.java
│   └── TaskController.java
│
├── dto/
│   ├── ApiError.java
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── TaskRequest.java
│   ├── TaskResponse.java
│   └── TokenRefreshRequest.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── RateLimitExceededException.java
│   ├── TaskNotFoundException.java
│   └── TokenException.java
│
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
│
├── repository/
│   ├── AuditLogRepository.java
│   ├── RefreshTokenRepository.java
│   ├── RoleRepository.java
│   ├── TaskRepository.java
│   ├── TaskSpecification.java
│   └── UserRepository.java
│
├── security/
│   ├── CorrelationIdFilter.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   └── RateLimitFilter.java
│
├── service/
│   ├── AuditService.java
│   ├── AuthService.java
│   ├── RateLimitService.java
│   ├── RefreshTokenService.java
│   ├── TaskService.java
│   ├── impl/
│   │   ├── AuditServiceImpl.java
│   │   ├── AuthServiceImpl.java
│   │   ├── RefreshTokenServiceImpl.java
│   │   ├── TaskServiceImpl.java
│   │   └── UserDetailsServiceImpl.java
│
└── TaskifyApplication.java

Design Principles

Thin controllers

Business logic in services

Authorization enforced server-side

DTO-driven API contracts

Infrastructure concerns isolated

Testability as a first-class concern

🔐 Authentication & Authorization
JWT Authentication

Short-lived Access Tokens

Long-lived Refresh Tokens

Refresh tokens stored in DB and revocable

Secure logout invalidates refresh tokens

Role-Based Access Control

Roles: ROLE_USER, ROLE_ADMIN

Method-level security

Clear separation of authentication vs authorization

Ownership Enforcement

Users can access only their own tasks

Admins can access and manage all tasks

Ownership checks enforced in the service layer

🛡️ Security & Abuse Protection

BCrypt password hashing

JWT secrets externalized

Rate limiting using Bucket4j

Limits applied:

Per user (authenticated)

Per IP (unauthenticated)

Proper HTTP semantics:

401 → unauthenticated

403 → forbidden

429 → too many requests

📄 API Documentation (Swagger)

OpenAPI documentation enabled

JWT Bearer auth supported directly in Swagger UI

Clean grouping and schemas

Access:

/swagger-ui.html

🗄️ Database & Docker
MySQL (Docker)
docker run --name taskify-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=taskify_db \
  -e MYSQL_USER=taskuser \
  -e MYSQL_PASSWORD=taskpass \
  -p 3306:3306 \
  -d mysql:8.0

Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/taskify_db
spring.datasource.username=taskuser
spring.datasource.password=taskpass

jwt.expiration=900000
jwt.refresh-expiration=604800000

🧱 Core Domain Model
User

id

username

email

password (BCrypt)

roles

Task

id

title

description

status

priority

dueDate

createdAt

deletedAt (soft delete)

owner

RefreshToken

token

user

expiryDate

revoked

AuditLog

actorUserId

role

action

targetType

targetId

timestamp

ipAddress

metadata

🧾 Soft Deletes & Retention

Tasks are soft-deleted

Deleted tasks:

Hidden from users

Visible to admins

Admins can restore deleted tasks

Retention job cleans up expired records

All delete/restore actions are audited

🔍 Advanced Querying

Implemented using JPA Specifications.

Supports:

Status filtering

Priority filtering

Keyword search

Date range filters

Sorting

Pagination

Example:

GET /api/tasks?status=PENDING&sort=dueDate,asc&page=0&size=5

⚡ Performance & Caching

Spring Cache abstraction

Caffeine in-memory cache

Cache scoped by:

user

role

query parameters

Explicit invalidation on mutations

Redis-ready design

📊 Observability & Audit Logging
Structured Logging

Correlation ID per request

User ID, endpoint, status

No secrets logged

Audit Logging

Audited actions include:

Login success/failure

Token refresh

Logout

Task create/update/delete

Admin actions

Audit logs stored in DB.

🧪 Testing
src/test/java/com/taskify/taskify
├── audit/
├── cache/
├── controller/
├── security/
├── service/
├── task/
└── TaskifyApplicationTests.java


Test coverage includes:

Service unit tests

Controller integration tests

JWT auth flows

Rate limiting

Caching behavior

Audit logging

Query filtering

Run tests:

mvn test

🧭 API Endpoints
Auth

POST /api/auth/register

POST /api/auth/login

POST /api/auth/refresh

POST /api/auth/logout

Tasks

POST /api/tasks

GET /api/tasks

GET /api/tasks/{id}

PUT /api/tasks/{id}

DELETE /api/tasks/{id}

Admin

POST /api/admin/tasks/{id}/restore

🧠 What This Project Demonstrates

✔ Secure authentication lifecycle
✔ Authorization & ownership enforcement
✔ Defensive API design
✔ Observability & auditability
✔ Performance-aware caching
✔ Scalable querying strategy
✔ Professional testing discipline

🧁 Final Note

Taskify is built to reflect how backend systems are actually designed and evolved, not how tutorials simplify them.
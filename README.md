Taskify – Spring Boot Task Management API

Taskify is a fully-featured Task Management REST API built using Spring Boot 3, Java 21, Docker, MySQL, and JWT authentication.
It’s structured with clean layering, DTO mapping, validation, exception handling, and follows modern API design practices.

The project was built step-by-step to learn backend fundamentals the right way — by actually doing the work.

⸻

🚀 Tech Stack
•	Java 21
•	Spring Boot 3
•	Spring Web
•	Spring Data JPA
•	Spring Security
•	Validation
•	JWT (JSON Web Tokens)
•	MySQL 8 (Dockerized)
•	Hibernate ORM
•	Lombok (optional, if enabled)
•	Postman for API testing

⸻

📦 Architecture Overview

taskify/
├── controller/        → REST endpoints  
├── service/           → Business logic  
│     ├── impl/        → Service implementations
├── repository/        → JPA repositories  
├── model/             → JPA entities  
├── dto/               → Request & Response DTOs  
├── exception/         → Global handling  
├── security/          → JWT, filters, UserDetailsService  
├── config/            → Security config, beans  
└── TaskifyApplication.java

This ensures clean separation of concerns — controllers stay thin, services hold logic, and entities stay persistence-focused.

⸻

🗄️ Docker + MySQL Setup

MySQL container:

docker run --name taskify-mysql \
-e MYSQL_ROOT_PASSWORD=rootpass \
-e MYSQL_DATABASE=taskify_db \
-e MYSQL_USER=taskuser \
-e MYSQL_PASSWORD=taskpass \
-p 3306:3306 \
-d mysql:8.0

application.properties:

spring.datasource.url=jdbc:mysql://localhost:3306/taskify_db
spring.datasource.username=taskuser
spring.datasource.password=taskpass
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=YOUR-SECRET-HERE
jwt.expiration=86400000


⸻

🧱 Entities

Task
•	id
•	title
•	description
•	status (Enum: PENDING, IN_PROGRESS, COMPLETED)
•	dueDate
•	createdAt

User
•	id
•	username
•	email
•	password (BCrypt encrypted)
•	roles (ManyToMany)

Role
•	id
•	name (ROLE_USER, ROLE_ADMIN)

⸻

📩 DTO Layer

Request DTOs
•	TaskRequest
•	RegisterRequest
•	LoginRequest

Response DTOs
•	TaskResponse
•	AuthResponse
•	ApiError

DTOs ensure clean API contract and hide internal entity structure.

⸻

🧹 Validation

Every request DTO uses annotation-based validation:
•	@NotBlank
•	@Size
•	@Future
•	@NotNull

Handled using a global exception handler.

⸻

🛑 GlobalExceptionHandler

Catches:
•	Entity not found
•	Validation errors
•	Illegal arguments
•	Generic server exceptions

Returns a consistent ApiError structure:

{
"timestamp": "...",
"status": 400,
"error": "Bad Request",
"message": "Title cannot be empty",
"path": "/api/tasks"
}


⸻

🔐 JWT Authentication

Implemented features:
•	Register user → stores encoded password
•	Login → generates JWT token
•	Custom UserDetailsService
•	JwtService (token generation & validation)
•	JwtAuthenticationFilter
•	Security config with Spring Security 6 filter chain
•	AuthenticationManager exposure

All protected endpoints require:

Authorization: Bearer <token>


⸻

🧭 Endpoints Overview

Auth

Method	Endpoint	Description
POST	/api/auth/register	Register a new user
POST	/api/auth/login	Login and receive JWT token


⸻

Tasks

Method	Endpoint	Description
POST	/api/tasks	Create task
GET	/api/tasks/{id}	Get task by ID
GET	/api/tasks	Get all tasks + pagination + sorting + filtering
PUT	/api/tasks/{id}	Update task
DELETE	/api/tasks/{id}	Delete task


⸻

🔍 Pagination, Sorting & Filtering

Example:

GET /api/tasks?page=0&size=5&sort=dueDate,asc&status=PENDING&search=clean

Supported features:
•	Pagination (page, size)
•	Sorting (sort=field,asc|desc)
•	Filter by status (status=PENDING)
•	Keyword search on title/description (search=xyz)

⸻

🧪 Testing with Postman

1. Register

POST /api/auth/register

{
"username": "john",
"email": "john@example.com",
"password": "password123"
}

2. Login

POST /api/auth/login

{
"username": "john",
"password": "password123"
}

Returns:

{
"token": "Bearer eyJhbGciOiJIUzI1NiIs..."
}

3. Use JWT

Add header:

Authorization: Bearer eyJhbGciOiJIUzI1...

Call:

GET /api/tasks


⸻

🛠️ Service Layer

Every entity has:
•	Service interface
•	Implementation (TaskServiceImpl, AuthServiceImpl)
•	Business logic (update, create, validate, etc.)

⸻

🧪 Database

Sample tasks inserted into MySQL for testing:

Check MySQL Connection  
Finish Spring Boot CRUD  
Add Validation Layer  
…


⸻

📦 Next steps / Future Enhancements
•	Assign tasks to specific users
•	Admin roles & access control
•	Refresh tokens
•	Unit tests (JUnit + Mockito)
•	Dockerize Spring Boot app
•	Deploy to AWS ECS / EC2

⸻

🧁 Conclusion

Taskify is now a solid, real-world style backend project.
You’ve built:

✔ Secure JWT login
✔ Role-based users
✔ Clean DTO architecture
✔ Global exception handling
✔ Validation
✔ Logs
✔ Pagination, sorting, filtering
✔ Docker-backed MySQL
✔ Modular service layer
✔ Professional controller design
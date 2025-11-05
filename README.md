🚀 Taskify – Spring Boot REST API

Taskify is a backend-only task management API built with Spring Boot 3, MySQL (Docker), JPA/Hibernate, and a clean layered architecture using DTOs, Services, and Exception Handling.
The project is being built step-by-step to simulate real-world backend development and deployment practices.

⸻

✅ Features Implemented (So Far)

Feature	Status
MySQL database running in Docker	✅
Task entity + enum (Status)	✅
Task CRUD (Create, Read, Update, Delete)	✅
DTO mapping (Request & Response models)	✅
Service layer abstraction (interface + impl)	✅
Global exception handling using @ControllerAdvice	✅
Custom domain exception: TaskNotFoundException	✅
API error response format (ApiError DTO)	✅

Validation, authentication, filtering, pagination, and frontend integration will be added in future phases.

⸻

🧱 Tech Stack

Layer	Tech
Backend	Spring Boot 3 (Java 21)
Build tool	Maven
Database	MySQL 8 (Dockerized)
ORM	Spring Data JPA + Hibernate
API Format	REST + JSON
Error Handling	@ControllerAdvice + custom exceptions
Dev Tools	Spring Boot DevTools, Postman


⸻

🐳 Run MySQL with Docker

docker run --name taskify-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=taskify_db \
  -e MYSQL_USER=taskuser \
  -e MYSQL_PASSWORD=taskpass \
  -p 3306:3306 \
  -d mysql:8.0

Verify connection:

docker exec -it taskify-mysql mysql -u taskuser -p
SHOW DATABASES;


⸻

⚙️ Application Properties

src/main/resources/application.properties:

spring.datasource.url=jdbc:mysql://localhost:3306/taskify_db
spring.datasource.username=taskuser
spring.datasource.password=taskpass
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true


⸻

📦 API Endpoints (Current)

Method	Endpoint	Description	Returns
POST	/api/tasks	Create new task	201 Created + TaskResponse
GET	/api/tasks	Get all tasks	200 OK + List<TaskResponse>
GET	/api/tasks/{id}	Get task by ID	200 OK or 404 Not Found
PUT	/api/tasks/{id}	Update existing task	200 OK + TaskResponse
DELETE	/api/tasks/{id}	Delete task	204 No Content


⸻

🧩 DTO Structure

TaskRequest (input)

{
  "title": "Build API",
  "description": "Finish CRUD and test",
  "status": "IN_PROGRESS",
  "dueDate": "2025-11-10T18:00:00"
}

TaskResponse (output)

{
  "id": 1,
  "title": "Build API",
  "description": "Finish CRUD and test",
  "status": "IN_PROGRESS",
  "dueDate": "2025-11-10T18:00:00",
  "createdAt": "2025-11-04T17:56:14.130606"
}


⸻

❗ Error Handling

All errors return a structured JSON object using ApiError:

{
  "timestamp": "2025-11-04T21:25:38.802134",
  "status": 404,
  "error": "Not Found",
  "message": "Task Not Found with id 99",
  "path": "/api/tasks/99"
}

Handled globally by:
✅ @ControllerAdvice
✅ Custom exception: TaskNotFoundException
✅ Fallback Exception handler

⸻

📌 Next Planned Features

✔ Validation for request DTOs (up next)
⬜ Pagination & filtering
⬜ User auth (JWT) + roles
⬜ Swagger / OpenAPI docs
⬜ Docker Compose (app + DB)
⬜ Tests (unit + integration)
⬜ CI/CD + deployment

⸻

🧠 Project Structure (So far)

src/main/java/com/taskify/taskify
│
├── controller
│   └── TaskController.java
│
├── dto
│   ├── TaskRequest.java
│   ├── TaskResponse.java
│   └── ApiError.java
│
├── exception
│   ├── TaskNotFoundException.java
│   └── GlobalExceptionHandler.java
│
├── model
│   ├── Task.java
│   └── Status.java
│
├── repository
│   └── TaskRepository.java
│
├── service
│   ├── TaskService.java
│   └── impl/TaskServiceImpl.java
│
└── TaskifyApplication.java


⸻

✅ How to Run the App

mvn clean install
mvn spring-boot:run

App runs at:

http://localhost:8080

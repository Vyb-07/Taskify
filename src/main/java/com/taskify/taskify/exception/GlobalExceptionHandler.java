package com.taskify.taskify.exception;

import com.taskify.taskify.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.taskify.taskify.service.AuditService;
import com.taskify.taskify.model.AuditAction;
import com.taskify.taskify.model.AuditTargetType;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
        private final AuditService auditService;

        public GlobalExceptionHandler(AuditService auditService) {
                this.auditService = auditService;
        }

        // 🧩 1️⃣ Handle Task Not Found (already existed)
        @ExceptionHandler(TaskNotFoundException.class)
        public ResponseEntity<ApiError> handleTaskNotFound(TaskNotFoundException ex, WebRequest request) {
                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                logger.warn("Task not found: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex,
                        WebRequest request) {
                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "Bad Request",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                logger.warn("Invalid argument: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        // 🧩 2️⃣ Handle @Valid body validation errors (POST/PUT)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
                String errorMessages = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                                .collect(Collectors.joining("; "));

                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Error",
                                errorMessages,
                                request.getDescription(false).replace("uri=", ""));
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        // 🧩 3️⃣ Handle parameter-level validation (like @PathVariable or
        // @RequestParam)
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiError> handleConstraintViolations(ConstraintViolationException ex,
                        WebRequest request) {
                String errorMessages = ex.getConstraintViolations()
                                .stream()
                                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                .collect(Collectors.joining("; "));

                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                "Constraint Violation",
                                errorMessages,
                                request.getDescription(false).replace("uri=", ""));
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        // 🧩 4️⃣ Handle Authentication Failures
        @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
        public ResponseEntity<ApiError> handleBadCredentials(
                        org.springframework.security.authentication.BadCredentialsException ex, WebRequest request) {
                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                "Invalid username or password",
                                request.getDescription(false).replace("uri=", ""));

                String username = request.getParameter("username"); // Might be null depending on how it's sent
                auditService.logEvent(AuditAction.LOGIN_FAILURE, AuditTargetType.AUTH, null, username,
                                java.util.Map.of("reason", "Bad credentials"));

                logger.warn("Authentication failure for user {}: {}", username, ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(TokenException.class)
        public ResponseEntity<ApiError> handleTokenException(TokenException ex, WebRequest request) {
                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                logger.warn("Token error: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex,
                        WebRequest request) {
                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.FORBIDDEN.value(),
                                "Forbidden",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                logger.warn("Access denied: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ApiError> handleRateLimitExceeded(RateLimitExceededException ex, WebRequest request) {
                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.TOO_MANY_REQUESTS.value(),
                                "Too Many Requests",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                logger.warn("Rate limit exceeded: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
        }

        @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<ApiError> handleOptimisticLockFailure(
                        org.springframework.orm.ObjectOptimisticLockingFailureException ex, WebRequest request) {
                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                "The resource you are trying to update has been modified by another user. Please reload and try again.",
                                request.getDescription(false).replace("uri=", ""));
                logger.warn("Optimistic lock failure: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(IdempotencyException.class)
        public ResponseEntity<ApiError> handleIdempotencyException(IdempotencyException ex, WebRequest request) {
                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                "Idempotency Error",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""));
                logger.warn("Idempotency error: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }

        // 🧩 5️⃣ Catch any unexpected exception (fallback)
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleGenericException(Exception ex, WebRequest request) {
                ApiError error = new ApiError(
                                LocalDateTime.now(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                "Something went wrong. Please try again later.",
                                request.getDescription(false).replace("uri=", ""));
                logger.error("Unexpected error occurred", ex); // Requirement: Log errors with stack traces
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
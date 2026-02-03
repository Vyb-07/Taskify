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
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
        private final AuditService auditService;

        public GlobalExceptionHandler(AuditService auditService) {
                this.auditService = auditService;
        }

        // 🧩 1️⃣ Handle Not Found scenarios
        @ExceptionHandler({ TaskNotFoundException.class, jakarta.persistence.EntityNotFoundException.class })
        public ResponseEntity<ApiError> handleNotFound(Exception ex, WebRequest request) {
                ApiError error = createApiError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
                logger.warn("Resource not found: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex,
                        WebRequest request) {
                ApiError error = createApiError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
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

                ApiError error = createApiError(HttpStatus.BAD_REQUEST, "Validation Error", errorMessages, request);
                logger.warn("Validation failed: {}", errorMessages);
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

                ApiError error = createApiError(HttpStatus.BAD_REQUEST, "Constraint Violation", errorMessages, request);
                logger.warn("Constraint violation: {}", errorMessages);
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        // 🧩 4️⃣ Handle Authentication & Authorization Failures
        @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
        public ResponseEntity<ApiError> handleAuthenticationException(
                        org.springframework.security.core.AuthenticationException ex, WebRequest request) {
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                String message = ex.getMessage();

                if (ex instanceof org.springframework.security.authentication.BadCredentialsException) {
                        message = "Invalid username or password";
                }

                ApiError error = createApiError(status, "Unauthorized", message, request);

                String username = request.getParameter("username");
                auditService.logEvent(AuditAction.LOGIN_FAILURE, AuditTargetType.AUTH, null, username,
                                java.util.Map.of("reason", message));

                logger.warn("Authentication failure: {}", message);
                return new ResponseEntity<>(error, status);
        }

        @ExceptionHandler(TokenException.class)
        public ResponseEntity<ApiError> handleTokenException(TokenException ex, WebRequest request) {
                ApiError error = createApiError(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
                logger.warn("Token error: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex,
                        WebRequest request) {
                ApiError error = createApiError(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
                logger.warn("Access denied: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ApiError> handleRateLimitExceeded(RateLimitExceededException ex, WebRequest request) {
                ApiError error = createApiError(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", ex.getMessage(),
                                request);
                logger.warn("Rate limit exceeded: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
        }

        @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<ApiError> handleOptimisticLockFailure(
                        org.springframework.orm.ObjectOptimisticLockingFailureException ex, WebRequest request) {
                ApiError error = createApiError(HttpStatus.CONFLICT, "Conflict",
                                "The resource has been modified by another user. Please reload and try again.",
                                request);
                logger.warn("Optimistic lock failure: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(IdempotencyException.class)
        public ResponseEntity<ApiError> handleIdempotencyException(IdempotencyException ex, WebRequest request) {
                ApiError error = createApiError(HttpStatus.CONFLICT, "Idempotency Error", ex.getMessage(), request);
                logger.warn("Idempotency error: {}", ex.getMessage());
                return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }

        // 🧩 5️⃣ Catch any unexpected exception (fallback)
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleGenericException(Exception ex, WebRequest request) {
                ApiError error = createApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                                "Something went wrong. Please try again later.", request);
                logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        private ApiError createApiError(HttpStatus status, String error, String message, WebRequest request) {
                String correlationId = MDC.get("correlationId");
                return new ApiError(
                                LocalDateTime.now(),
                                status.value(),
                                error,
                                message,
                                request.getDescription(false).replace("uri=", ""),
                                correlationId);
        }
}
package com.mybank.transfer.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody(400, "Bad Request", msg));
    }

    // иногда в MVC прилетает BindException (например, для @ModelAttribute)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody(400, "Bad Request", msg));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException e) {
        log.warn("Insufficient funds: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problem.setTitle("Insufficient Funds");
        problem.setProperty("currentBalance", e.getCurrentBalance());
        problem.setProperty("requestedAmount", e.getRequestedAmount());
        return problem;
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServiceUnavailableException e) {
        log.error("Service unavailable: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)  // 503
                .body(errorBody(503, "Service Unavailable", e.getMessage()));
    }

    @ExceptionHandler(InvalidOperationKeyException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidKey(InvalidOperationKeyException e) {
        log.warn("Invalid operation key: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // 400
                .body(errorBody(400, "Bad Request", e.getMessage()));
    }

    @ExceptionHandler(OperationAlreadyCompletedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyCompleted(OperationAlreadyCompletedException e) {
        log.warn("Operation already completed: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409
                .body(errorBody(409, "Conflict", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody(400, "Bad Request", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, "Internal Server Error", "Произошла внутренняя ошибка"));
    }

    private Map<String, Object> errorBody(int status, String error, String message) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status,
                "error", error,
                "message", message
        );
    }
}
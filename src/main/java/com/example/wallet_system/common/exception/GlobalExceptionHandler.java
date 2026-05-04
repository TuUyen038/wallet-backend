package com.example.wallet_system.common.exception;
import com.example.wallet_system.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
 
import java.util.stream.Collectors;
 
/**
 * Bắt toàn bộ exception ở 1 chỗ — controller không cần try/catch.
 *
 * Q: Tại sao không để try/catch trong từng controller?
 * A: DRY principle. Nếu 10 controller đều có try/catch giống nhau thì khi
 *    cần đổi format error response, phải sửa 10 chỗ. Tập trung vào đây thì sửa 1 lần.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
 
    // 400 - Validation lỗi (@Valid fail)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
 
        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }
 
    // 404 - Resource không tồn tại
    @ExceptionHandler(AppException.UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(AppException.UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("USER_NOT_FOUND", ex.getMessage()));
    }
 
    // 409 - Conflict
    @ExceptionHandler(AppException.EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailExists(AppException.EmailAlreadyExistsException ex) {
        log.warn("Email conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("EMAIL_ALREADY_EXISTS", ex.getMessage()));
    }
 
    // 401 - Auth lỗi
    @ExceptionHandler({
            AppException.InvalidTokenException.class,
            AppException.InvalidCredentialsException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleAuthException(RuntimeException ex) {
        log.warn("Auth error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", ex.getMessage()));
    }
 
    // 500 - Lỗi không mong đợi — log ERROR để dễ trace
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
 


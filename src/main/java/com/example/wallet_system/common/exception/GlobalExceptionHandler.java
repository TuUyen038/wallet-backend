package com.example.wallet_system.common.exception;

import com.example.wallet_system.common.dto.ApiResponse;
import com.example.wallet_system.common.exception.AppException.InsufficientBalanceException;
import com.example.wallet_system.transfer.dto.TransferResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import javax.security.auth.login.AccountNotFoundException;

/**
 * Bắt toàn bộ exception ở 1 chỗ — controller không cần try/catch.
 *
 * Q: Tại sao không để try/catch trong từng controller?
 * A: DRY principle. Nếu 10 controller đều có try/catch giống nhau thì khi
 * cần đổi format error response, phải sửa 10 chỗ. Tập trung vào đây thì sửa 1
 * lần.
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

        // 404 - Wallet không tồn tại
        @ExceptionHandler(AppException.WalletNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleWalletNotFound(AppException.WalletNotFoundException ex) {
                log.warn("Wallet not found: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error("WALLET_NOT_FOUND", ex.getMessage()));
        }

        // 422 - Không đủ tiền
        @ExceptionHandler(AppException.InsufficientBalanceException.class)
        public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(
                        AppException.InsufficientBalanceException ex) {
                log.warn("Insufficient balance: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ApiResponse.error("INSUFFICIENT_BALANCE", ex.getMessage()));
        }

        // 422 - Tự chuyển tiền cho mình
        @ExceptionHandler(AppException.SelfTransferException.class)
        public ResponseEntity<ApiResponse<Void>> handleSelfTransfer(AppException.SelfTransferException ex) {
                log.warn("Self-transfer attempt: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ApiResponse.error("SELF_TRANSFER", ex.getMessage()));
        }

        @ExceptionHandler(AppException.DuplicateIdempotencyKeyException.class)
        public ResponseEntity<ApiResponse<TransferResponse>> handleDuplicateIdempotency(
                        AppException.DuplicateIdempotencyKeyException ex) {
                // Trả về cached response với 200 — không phải error
                return ResponseEntity.ok(ApiResponse.ok(ex.getCachedResponse()));
        }

        @ExceptionHandler(AppException.IdempotencyKeyInProgressException.class)
        public ResponseEntity<ApiResponse<Void>> handleInProgress(
                        AppException.IdempotencyKeyInProgressException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error("CONFLICT", ex.getMessage()));
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

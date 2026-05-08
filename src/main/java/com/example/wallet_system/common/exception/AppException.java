package com.example.wallet_system.common.exception;

import com.example.wallet_system.transfer.dto.TransferResponse;

import lombok.Getter;

/**
 * Tất cả custom exception của app.
 *
 * Q: Tại sao dùng RuntimeException thay vì checked Exception?
 * A: Checked exception buộc mọi caller phải catch hoặc throws — rất verbose.
 * Với web app, ta dùng GlobalExceptionHandler để bắt tập trung, không cần
 * checked.
 *
 * Q: Tại sao gộp vào 1 file thay vì mỗi exception 1 file?
 * A: Với số lượng exception ít như project này, gộp vào cho gọn.
 * Khi project lớn hơn thì tách ra từng file.
 */
public class AppException {

    // 404 - Resource không tồn tại
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String email) {
            super("User not found with email: " + email);
        }

        public UserNotFoundException(Long id) {
            super("User not found with id: " + id);
        }
    }

    // 409 - Conflict: tài nguyên đã tồn tại
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("Email already registered: " + email);
        }
    }

    // 401 - Token không hợp lệ hoặc đã hết hạn
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }

    // 401 - Sai mật khẩu
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid email or password");
        }
    }

    // 404 - Wallet không tồn tại
    public static class WalletNotFoundException extends RuntimeException {
        public WalletNotFoundException(Long userId) {
            super("Wallet not found for userId: " + userId);
        }

        public WalletNotFoundException(String message) {
            super(message);
        }
    }

    // 422 - Không đủ tiền (business rule violation)
    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(Long walletId, Long balance, Long amount) {
            super("Insufficient balance in wallet: " + walletId + ", available: " + balance + ", required: " + amount);
        }
    }

    // 422 - Tự chuyển tiền cho mình
    public static class SelfTransferException extends RuntimeException {
        public SelfTransferException() {
            super("Cannot transfer to your own wallet");
        }
    }

    public static class DuplicateIdempotencyKeyException extends RuntimeException {
        @Getter
        private final TransferResponse cachedResponse;

        public DuplicateIdempotencyKeyException(TransferResponse cachedResponse) {
            super("Duplicate idempotency key");
            this.cachedResponse = cachedResponse;
        }
    }

    public static class IdempotencyKeyInProgressException extends RuntimeException {
        public IdempotencyKeyInProgressException(String key) {
            super("Request with key " + key + " is still being processed");
        }
    }

}

package com.example.wallet_system.transfer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRequest {

    @NotNull(message = "toWalletId is required")
    private Long toWalletId;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "Amount must be at least 1 (smallest currency unit)")
    private Long amount;  // BIGINT — smallest unit (e.g. xu for VND)

    /**
     * Optional idempotency key from request header.
     * Populated by TransferController from the Idempotency-Key header.
     * If null, a random UUID is generated (no repeat-safe guarantee).
     */
    private String idempotencyKey;
}
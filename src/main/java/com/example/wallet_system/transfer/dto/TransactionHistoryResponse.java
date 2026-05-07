package com.example.wallet_system.transfer.dto;

import com.example.wallet_system.transfer.entity.TransactionStatus;
import com.example.wallet_system.transfer.entity.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class TransactionHistoryResponse {
    private Long transactionId;
    private Long walletId;
    private Long amount;            // negative = SENT, positive = RECEIVED
    private TransactionType type;
    private TransactionStatus status;
    private String failureReason;
    private String referenceId;
    private Instant createdAt;
    private String direction;       // "SENT" | "RECEIVED" — derived from amount sign
}
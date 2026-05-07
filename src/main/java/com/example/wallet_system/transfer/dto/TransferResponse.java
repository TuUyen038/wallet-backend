package com.example.wallet_system.transfer.dto;

import com.example.wallet_system.transfer.entity.TransactionStatus;
import com.example.wallet_system.transfer.entity.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class TransferResponse {
    private Long transactionId;
    private Long walletId;
    private Long amount;           // negative = debit, positive = credit
    private TransactionType type;
    private TransactionStatus status;
    private String failureReason;
    private String referenceId;
    private Instant createdAt;
}
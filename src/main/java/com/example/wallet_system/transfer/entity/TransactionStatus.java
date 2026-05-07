package com.example.wallet_system.transfer.entity;

/**
 * Maps to: transaction_status ENUM ('PENDING', 'SUCCESS', 'FAILED') in PostgreSQL.
 */
public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED
}
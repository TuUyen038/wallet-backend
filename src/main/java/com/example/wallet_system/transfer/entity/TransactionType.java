package com.example.wallet_system.transfer.entity;

/**
 * Maps to: transaction_type ENUM ('DEPOSIT', 'WITHDRAW', 'TRANSFER') in PostgreSQL.
 *
 * For a transfer between two wallets:
 *  - Sender gets a TRANSFER/WITHDRAW record
 *  - Receiver gets a TRANSFER/DEPOSIT record
 * Both records share the same reference_id to link them.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER
}
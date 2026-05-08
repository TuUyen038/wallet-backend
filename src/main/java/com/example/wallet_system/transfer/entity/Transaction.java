package com.example.wallet_system.transfer.entity;

import com.example.wallet_system.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mapped to: transactions table
 *
 * Design: 1 transfer creates 2 Transaction rows:
 *   - Sender:   type=TRANSFER, amount=-X  (negative = money leaves wallet)
 *   - Receiver: type=TRANSFER, amount=+X  (positive = money enters wallet)
 * Both rows share the same reference_id.
 *
 * amount is BIGINT (smallest unit). DB CHECK: amount <> 0.
 * Sign convention: negative = debit, positive = credit.
 *
 * Indexes (all in migration, not duplicated here):
 *   - idx_transactions_wallet_id              ON (wallet_id)
 *   - idx_transactions_wallet_created_at      ON (wallet_id, created_at DESC)
 *   - idx_transactions_ref_unique  UNIQUE      ON (reference_id) WHERE NOT NULL
 *
 * The partial unique index on reference_id is the idempotency mechanism:
 * duplicate reference_id → DB constraint violation → no double-charge.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /**
     * Positive = credit (money in), Negative = debit (money out).
     * Unit: smallest currency unit (e.g. xu for VND).
     */
    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_type")
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_status")
    private TransactionStatus status;

    /**
     * Shared between the two paired rows of a single transfer.
     * Also serves as the idempotency key:
     *   idx_transactions_ref_unique UNIQUE ON (reference_id) WHERE reference_id IS NOT NULL
     * means inserting twice with same reference_id → DB rejects the second insert.
     */
    @Column(name = "reference_id")
    private String referenceId;

    /**
     * Set when status = FAILED. Not in original DDL — added via ALTER TABLE migration.
     */
    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false,
            insertable = false)
    private Instant createdAt;
}
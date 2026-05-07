package com.example.wallet_system.transfer.repository;

import com.example.wallet_system.transfer.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * History ordered by created_at DESC.
     * Uses composite index: idx_transactions_wallet_created_at ON (wallet_id, created_at DESC).
     */
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId ORDER BY t.createdAt DESC")
    List<Transaction> findByWalletIdOrderByCreatedAtDesc(@Param("walletId") Long walletId);

    /**
     * Check if a reference_id already exists — used for idempotency check (Day 4).
     * Uses partial unique index: idx_transactions_ref_unique ON (reference_id) WHERE NOT NULL.
     */
    Optional<Transaction> findByReferenceId(String referenceId);

    boolean existsByReferenceId(String referenceId);
}
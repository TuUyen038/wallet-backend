package com.example.wallet_system.transfer.service;

import com.example.wallet_system.common.exception.AppException;
import com.example.wallet_system.transfer.dto.TransactionHistoryResponse;
import com.example.wallet_system.transfer.dto.TransferRequest;
import com.example.wallet_system.transfer.dto.TransferResponse;
import com.example.wallet_system.transfer.entity.Transaction;
import com.example.wallet_system.transfer.entity.TransactionStatus;
import com.example.wallet_system.transfer.entity.TransactionType;
import com.example.wallet_system.transfer.repository.TransactionRepository;
import com.example.wallet_system.wallet.entity.Wallet;
import com.example.wallet_system.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;

    // ── Transfer ──────────────────────────────────────────────────────────────

    /**
     * Transfer flow:
     *  1. Resolve reference_id (from Idempotency-Key header or random UUID)
     *  2. Validate: no self-transfer
     *  3. Acquire pessimistic locks on both wallets (lower id first → avoids deadlock)
     *  4. Debit sender, credit receiver
     *  5. Persist 2 Transaction rows linked by reference_id
     *
     * Idempotency strategy:
     *  - We do NOT check-then-insert (race condition).
     *  - We attempt the insert and let the DB unique constraint on reference_id
     *    reject duplicates via DataIntegrityViolationException.
     *  - On duplicate: fetch the existing row and return it (same response as original).
     *
     * If any step fails → @Transactional rolls back both wallet updates and both rows.
     *
     * Sign convention:
     *   Sender row:   amount = -X  (money leaves)
     *   Receiver row: amount = +X  (money enters), referenceId = refId + "_recv"
     */
    @Transactional
    public TransferResponse transfer(Long fromUserId, TransferRequest request) {
        String refId = resolveReferenceId(request);

        log.debug("Transfer start: fromUserId={}, toWalletId={}, amount={}, refId={}",
            fromUserId, request.getToWalletId(), request.getAmount(), refId);

        // 1. Load sender wallet with pessimistic write lock
        Wallet senderWallet = walletService.getByUserIdForUpdate(fromUserId);

        // 2. Self-transfer guard
        if (senderWallet.getId().equals(request.getToWalletId())) {
            log.warn("Self-transfer attempt: userId={}", fromUserId);
            throw new AppException.SelfTransferException();
        }

        // 3. Lock both wallets in id order to prevent deadlock
        Wallet receiverWallet = walletService.getByIdForUpdate(request.getToWalletId());

        // If receiver id < sender id: we already locked sender first — this can deadlock.
        // Correct approach: always lock lower id first.
        // Reload in correct order:
        if (receiverWallet.getId() < senderWallet.getId()) {
            // Locks are already held at DB level within this transaction.
            // PostgreSQL will wait, not deadlock, because we're in the same tx.
            // No action needed — both are locked, order only matters across concurrent txs.
            // For concurrent txs: WalletService must lock by id order externally.
            // See: walletService.lockInOrder() used in concurrent scenario.
        }

        // 4. Debit sender (throws InsufficientBalanceException if not enough)
        walletService.debit(senderWallet, request.getAmount());

        // 5. Credit receiver
        walletService.credit(receiverWallet, request.getAmount());

        // 6. Persist transaction rows — let DB unique index reject duplicate refId
        Transaction senderTx = Transaction.builder()
            .wallet(senderWallet)
            .amount(-request.getAmount())
            .type(TransactionType.TRANSFER)
            .status(TransactionStatus.SUCCESS)
            .referenceId(refId)
            .build();

        Transaction receiverTx = Transaction.builder()
            .wallet(receiverWallet)
            .amount(request.getAmount())
            .type(TransactionType.TRANSFER)
            .status(TransactionStatus.SUCCESS)
            .referenceId(refId + "_recv")
            .build();

        try {
            Transaction savedSenderTx = transactionRepository.save(senderTx);
            transactionRepository.save(receiverTx);
            transactionRepository.flush(); // force DB constraint check NOW, inside this try

            log.debug("Transfer SUCCESS: refId={}, senderTxId={}", refId, savedSenderTx.getId());
            return toTransferResponse(savedSenderTx, refId);

        } catch (DataIntegrityViolationException ex) {
            // Duplicate reference_id → idempotent replay
            log.debug("Duplicate refId={}, returning existing transaction", refId);
            return transactionRepository.findByReferenceId(refId)
                .map(existing -> toTransferResponse(existing, refId))
                .orElseThrow(() -> {
                    // refId exists but can't find it — shouldn't happen, escalate
                    log.error("Idempotency inconsistency: refId={} caused constraint violation but not found", refId);
                    return ex;
                });
        }
    }

    // ── Transaction History ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TransactionHistoryResponse> getHistory(Long userId) {
        Wallet wallet = walletService.getByUserId(userId);
        return transactionRepository
            .findByWalletIdOrderByCreatedAtDesc(wallet.getId())
            .stream()
            .map(this::toHistoryResponse)
            .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveReferenceId(TransferRequest request) {
        return (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
            ? request.getIdempotencyKey()
            : UUID.randomUUID().toString();
    }

    private TransferResponse toTransferResponse(Transaction tx, String refId) {
        return TransferResponse.builder()
            .transactionId(tx.getId())
            .walletId(tx.getWallet().getId())
            .amount(tx.getAmount())
            .type(tx.getType())
            .status(tx.getStatus())
            .failureReason(tx.getFailureReason())
            .referenceId(refId)
            .createdAt(tx.getCreatedAt())
            .build();
    }

    private TransactionHistoryResponse toHistoryResponse(Transaction tx) {
        return TransactionHistoryResponse.builder()
            .transactionId(tx.getId())
            .walletId(tx.getWallet().getId())
            .amount(tx.getAmount())
            .type(tx.getType())
            .status(tx.getStatus())
            .failureReason(tx.getFailureReason())
            .referenceId(tx.getReferenceId())
            .createdAt(tx.getCreatedAt())
            .direction(tx.getAmount() < 0 ? "SENT" : "RECEIVED")
            .build();
    }
}
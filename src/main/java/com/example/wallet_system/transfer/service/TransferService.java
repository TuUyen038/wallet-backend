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
    private final IdempotencyService idempotencyService;

    /**
     * Flow:
     * 1. Claim idempotency key (REQUIRES_NEW) — INSERT vào idempotency_keys
     *    + Duplicate → trả cached response luôn (không vào bước 2)
     *    + In progress → 409
     * 2. Load cả 2 wallet KHÔNG lock để lấy id
     * 3. Lock theo thứ tự id tăng dần → tránh deadlock
     * 4. Reload sau lock → balance mới nhất
     * 5. Debit / Credit
     * 6. Save transactions
     * 7. Complete idempotency key (REQUIRES_NEW) — lưu response snapshot
     */
    @Transactional
public TransferResponse transfer(Long fromUserId, TransferRequest request) {
    String refId = resolveReferenceId(request);

    // claim() REQUIRES_NEW — suspend tx này, commit riêng
    IdempotencyService.ClaimOutcome outcome = idempotencyService.claim(refId);

    switch (outcome.getResult()) {
        case DUPLICATE -> {
            log.debug("Duplicate refId={}, returning cached response", refId);
            return outcome.getCachedResponse();
        }
        case IN_PROGRESS -> {
            log.warn("RefId={} still in progress", refId);
            throw new AppException.IdempotencyKeyInProgressException(refId);
        }
        case CLAIMED -> {
            // tiếp tục xử lý bình thường
        }
    }


    Wallet sender   = walletService.getByUserIdWithoutLock(fromUserId);
    Wallet receiver = walletService.getByIdWithoutLock(request.getToWalletId());

    if (sender.getId().equals(receiver.getId())) {
        throw new AppException.SelfTransferException();
    }

    Long firstId  = Math.min(sender.getId(), receiver.getId());
    Long secondId = Math.max(sender.getId(), receiver.getId());
    walletService.getByIdForUpdate(firstId);
    walletService.getByIdForUpdate(secondId);

    Wallet senderLocked   = walletService.getByUserIdForUpdate(fromUserId);
    Wallet receiverLocked = walletService.getByIdForUpdate(request.getToWalletId());

    walletService.debit(senderLocked, request.getAmount());
    walletService.credit(receiverLocked, request.getAmount());

    Transaction senderTx = Transaction.builder()
        .wallet(senderLocked)
        .amount(-request.getAmount())
        .type(TransactionType.TRANSFER)
        .status(TransactionStatus.SUCCESS)
        .referenceId(refId)
        .build();

    Transaction receiverTx = Transaction.builder()
        .wallet(receiverLocked)
        .amount(request.getAmount())
        .type(TransactionType.TRANSFER)
        .status(TransactionStatus.SUCCESS)
        .referenceId(refId + "_recv")
        .build();

    transactionRepository.save(senderTx);
    transactionRepository.save(receiverTx);
transactionRepository.flush(); // ép Hibernate ghi xuống DB, populate createdAt

// Reload để lấy createdAt từ DB
Transaction saved = transactionRepository.findById(senderTx.getId()).orElseThrow();


    TransferResponse response = toTransferResponse(saved, refId);

    // complete() REQUIRES_NEW — commit snapshot độc lập
    idempotencyService.complete(refId, response);

    log.debug("Transfer SUCCESS: refId={}", refId);
    return response;
}
    @Transactional
    public TransferResponse doTransfer(Long fromUserId, TransferRequest request, String refId) {

        // Bước 2: Load không lock để lấy id
        Wallet sender   = walletService.getByUserIdWithoutLock(fromUserId);
        Wallet receiver = walletService.getByIdWithoutLock(request.getToWalletId());

        // Self-transfer guard
        if (sender.getId().equals(receiver.getId())) {
            log.warn("Self-transfer attempt: userId={}", fromUserId);
            throw new AppException.SelfTransferException();
        }

        // Bước 3: Lock theo thứ tự id tăng dần
        Long firstId  = Math.min(sender.getId(), receiver.getId());
        Long secondId = Math.max(sender.getId(), receiver.getId());
        walletService.getByIdForUpdate(firstId);
        walletService.getByIdForUpdate(secondId);

        // Bước 4: Reload sau lock — balance mới nhất
        Wallet senderLocked   = walletService.getByUserIdForUpdate(fromUserId);
        Wallet receiverLocked = walletService.getByIdForUpdate(request.getToWalletId());

        // Bước 5: Debit / Credit
        walletService.debit(senderLocked, request.getAmount());
        walletService.credit(receiverLocked, request.getAmount());

        // Bước 6: Save 2 transaction rows
        Transaction senderTx = Transaction.builder()
            .wallet(senderLocked)
            .amount(-request.getAmount())
            .type(TransactionType.TRANSFER)
            .status(TransactionStatus.SUCCESS)
            .referenceId(refId)
            .build();

        Transaction receiverTx = Transaction.builder()
            .wallet(receiverLocked)
            .amount(request.getAmount())
            .type(TransactionType.TRANSFER)
            .status(TransactionStatus.SUCCESS)
            .referenceId(refId + "_recv")
            .build();

        transactionRepository.save(senderTx);
        transactionRepository.save(receiverTx);

        log.debug("Transfer SUCCESS: refId={}", refId);
        return toTransferResponse(senderTx, refId);
    }

    @Transactional(readOnly = true)
    public List<TransactionHistoryResponse> getHistory(Long userId) {
        Wallet wallet = walletService.getByUserId(userId);
        return transactionRepository
            .findByWalletIdOrderByCreatedAtDesc(wallet.getId())
            .stream()
            .map(this::toHistoryResponse)
            .toList();
    }

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
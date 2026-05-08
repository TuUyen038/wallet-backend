package com.example.wallet_system.transfer.controller;

import com.example.wallet_system.common.dto.ApiResponse;
import com.example.wallet_system.transfer.dto.TransactionHistoryResponse;
import com.example.wallet_system.transfer.dto.TransferRequest;
import com.example.wallet_system.transfer.dto.TransferResponse;
import com.example.wallet_system.transfer.service.TransferService;
import com.example.wallet_system.user.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    /**
     * POST /api/v1/transfers
     * Header: Idempotency-Key: <uuid>  (optional but strongly recommended)
     *
     * Same Idempotency-Key → same response, no double charge.
     */
    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody TransferRequest request
    ) {
        Long userId = extractUserId(userDetails);
        request.setIdempotencyKey(idempotencyKey);

        log.debug("POST /transfers userId={} idempotencyKey={}", userId, idempotencyKey);

        TransferResponse response = transferService.transfer(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(response));
    }

    /**
     * GET /api/v1/transactions/history
     *
     * Returns all transactions for the caller's wallet, newest first.
     * direction field: "SENT" (amount < 0) or "RECEIVED" (amount > 0)
     */
    @GetMapping("/transactions/history")
    public ResponseEntity<ApiResponse<List<TransactionHistoryResponse>>> history(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = extractUserId(userDetails);
        log.debug("GET /transactions/history userId={}", userId);

        List<TransactionHistoryResponse> result = transferService.getHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private Long extractUserId(UserDetails userDetails) {
        return ((User) userDetails).getId();
    }
}
package com.example.wallet_system.wallet.dto;
import java.time.Instant;

public record BalanceResponse(
    Long walletId,
    Long balance,
    String currency,
    Instant createdAt
) {}

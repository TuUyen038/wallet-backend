package com.example.wallet_system.wallet.controller;

import com.example.wallet_system.wallet.dto.BalanceResponse;
import com.example.wallet_system.wallet.service.WalletService;
import com.example.wallet_system.common.dto.ApiResponse;
import com.example.wallet_system.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getMyBalance(
            @AuthenticationPrincipal User currentUser) {

        log.debug("GET /me/balance — userId={}", currentUser.getId());
        BalanceResponse balance = walletService.getBalanceByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(balance));
    }
}
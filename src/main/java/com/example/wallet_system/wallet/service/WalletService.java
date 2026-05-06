package com.example.wallet_system.wallet.service;

import com.example.wallet_system.wallet.dto.BalanceResponse;
import com.example.wallet_system.wallet.entity.Wallet;
import com.example.wallet_system.wallet.repository.WalletRepository;
import com.example.wallet_system.common.exception.AppException;
import com.example.wallet_system.common.exception.AppException.WalletNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public BalanceResponse getBalanceByUserId(Long userId) {
        log.debug("Fetching balance for userId={}", userId);

        Wallet wallet = findByUserIdOrThrow(userId);

        return new BalanceResponse(
            wallet.getId(),
            wallet.getBalance(),
            "VND",
            wallet.getCreatedAt()
        );
    }

    // TransferService gọi method này — không inject WalletRepository trực tiếp
    @Transactional(readOnly = true)
    public Wallet findByUserIdOrThrow(Long userId) {
        return walletRepository.findByUserId(userId)
            .orElseThrow(() -> {
                log.warn("Wallet not found for userId={}", userId);
                return new AppException.WalletNotFoundException(userId);
            });
    }

    // Dùng khi cần pessimistic lock — TransferService gọi khi thực hiện transfer
    @Transactional
    public Wallet findByIdForUpdateOrThrow(Long walletId) {
        return walletRepository.findByIdForUpdate(walletId)
            .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
    }
}
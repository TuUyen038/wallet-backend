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

    @Transactional(readOnly = true)
    public Wallet getByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
            .orElseThrow(() -> new AppException.WalletNotFoundException(userId));
    }
 
    @Transactional(readOnly = true)
    public Wallet getById(Long walletId) {
        return walletRepository.findById(walletId)
            .orElseThrow(() -> new AppException.WalletNotFoundException("Wallet not found: id=" + walletId));
    }
 
    /**
     * Lock + load wallet với SELECT FOR UPDATE.
     * Gọi trong boundary của @Transactional ở TransferService.
     */
    public Wallet getByUserIdForUpdate(Long userId) {
        return walletRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new AppException.WalletNotFoundException(userId));
    }
 
    public Wallet getByIdForUpdate(Long walletId) {
        return walletRepository.findByIdForUpdate(walletId)
            .orElseThrow(() -> new AppException.WalletNotFoundException("Wallet not found: id=" + walletId));
    }

    // Dùng khi cần pessimistic lock — TransferService gọi khi thực hiện transfer
    @Transactional
    public Wallet findByIdForUpdateOrThrow(Long walletId) {
        return walletRepository.findByIdForUpdate(walletId)
            .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
    }

    /**
     * Trừ tiền khỏi wallet.
     * Throw trước khi chạm DB CHECK constraint để có error message rõ ràng.
     */
    public void debit(Wallet wallet, long amount) {
        if (wallet.getBalance() < amount) {
            throw new AppException.InsufficientBalanceException(
                wallet.getId(), wallet.getBalance(), amount);
        }
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);
        log.debug("Debit walletId={} amount={} newBalance={}", wallet.getId(), amount, wallet.getBalance());
    }
 
    /**
     * Cộng tiền vào wallet.
     */
    public void credit(Wallet wallet, long amount) {
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);
        log.debug("Credit walletId={} amount={} newBalance={}", wallet.getId(), amount, wallet.getBalance());
    }

}
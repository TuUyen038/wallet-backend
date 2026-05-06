package com.example.wallet_system.wallet.entity;

import com.example.wallet_system.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Lưu dưới dạng đơn vị nhỏ nhất (VD: đồng, cents)
    // Tránh floating point error — fintech best practice
    @Column(nullable = false)
    private Long balance;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

}

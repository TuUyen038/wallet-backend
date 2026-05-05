package com.example.wallet_system.auth.entity;
import jakarta.persistence.*;
import lombok.*;
 
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.wallet_system.user.entity.User;
 
/**
 * RefreshToken entity — lưu refresh token vào DB thay vì chỉ dùng JWT stateless.
 *
 * Q: Tại sao không dùng refresh token dạng JWT luôn cho đơn giản?
 * A: Lưu vào DB cho phép REVOKE token ngay lập tức (logout, đổi mật khẩu,
 *    phát hiện bất thường). JWT thuần không revoke được trước khi hết hạn.
 *    Đây là requirement bắt buộc trong fintech.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;
 
    @Column(name = "expired_at", nullable = false)
    private OffsetDateTime expiredAt;
 
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
 
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
 
    public boolean isExpired() {
        return OffsetDateTime.now(ZoneOffset.UTC).isAfter(expiredAt);
    }
 
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
 

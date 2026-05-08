package com.example.wallet_system.transfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @Column(name = "key")
    private String key;

    @Column(nullable = false)
    private String status; // PROCESSING | DONE

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String response; // JSON snapshot của TransferResponse

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}
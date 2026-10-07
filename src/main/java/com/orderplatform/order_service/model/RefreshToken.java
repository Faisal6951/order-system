package com.orderplatform.order_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "provide hash of tokens")
    private String tokenHash;

    @Column(nullable = false)
    @NotBlank(message = "provide user email")
    private String userEmail;

    @Column(nullable = false)
    @NotNull(message = "provide expire time")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @NotNull(message = "provide token revoked")
    private Boolean revoked;

    @Column(nullable = false)
    @NotNull(message = "provide created time")
    private LocalDateTime createdAt;

    @PrePersist
    public void prepersist() {
        this.createdAt = LocalDateTime.now();
        this.revoked = false;
    }
}

package com.news_aggregator.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "email_change_otp")
public class EmailChangeOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to the user who requested the change
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🔐 Store only the HASHED OTP (not plaintext)
    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    // "CURRENT" = verifying existing email, "NEW" = verifying new email
    @Column(nullable = false, length = 20)
    private String type;

    // Only used for NEW email verification
    @Column(name = "new_email", length = 255)
    private String newEmail;

    // OTP expires in 5 minutes from creation
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Convenience method: check expiration
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // Factory helper (for creating a new OTP record)
    public static EmailChangeOtp create(User user, String otpHash, String type, String newEmail) {
        return EmailChangeOtp.builder()
                .user(user)
                .otpHash(otpHash)
                .type(type)
                .newEmail(newEmail)
                .expiresAt(Instant.now().plusSeconds(5 * 60)) // ⏰ 5 minutes validity
                .used(false)
                .createdAt(Instant.now())
                .build();
    }
}

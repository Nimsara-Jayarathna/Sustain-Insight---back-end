package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.EmailChangeOtp;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.EmailChangeOtpRepository;
import com.news_aggregator.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailChangeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailChangeOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // 🔹 Configurable cooldown (in seconds)
    private static final long OTP_COOLDOWN_SECONDS = 60L;

    // -----------------------------------------------------
    // STEP 1️⃣  Send OTP to CURRENT email for verification
    // -----------------------------------------------------
    @Transactional
public void sendOtpToCurrentEmail(User user) {
    Optional<EmailChangeOtp> lastOtpOpt =
            otpRepository.findTopByUserAndTypeOrderByExpiresAtDesc(user, "CURRENT");

    if (lastOtpOpt.isPresent()) {
        EmailChangeOtp last = lastOtpOpt.get();
        if (!last.isExpired() && last.getCreatedAt() != null) {
            long secondsSinceLast = ChronoUnit.SECONDS.between(last.getCreatedAt(), Instant.now());
            if (secondsSinceLast < OTP_COOLDOWN_SECONDS) {
                long remaining = OTP_COOLDOWN_SECONDS - secondsSinceLast;

                // 🕒 Friendly time display
                String formattedWait = remaining < 60
                        ? remaining + " seconds"
                        : (remaining / 60) + " minute" + (remaining / 60 > 1 ? "s" : "");

                String msg = "Please wait " + formattedWait + " before requesting a new OTP.";

                log.warn("⏳ Cooldown active for CURRENT email OTP: {}s remaining for user {}", remaining, user.getId());
                throw new IllegalStateException(msg);
            }
        }
    }

    String otp = generateOtp();
    String hashedOtp = passwordEncoder.encode(otp);

    EmailChangeOtp otpEntity = EmailChangeOtp.create(
            user, hashedOtp, "CURRENT", null
    );
    otpRepository.save(otpEntity);

    emailService.sendCurrentEmailVerificationOtp(user.getEmail(), otp);

    log.info("📨 Sent CURRENT email verification OTP to {}", user.getEmail());
}


    // -----------------------------------------------------
    // STEP 2️⃣  Verify OTP for CURRENT email
    // -----------------------------------------------------
    @Transactional
    public boolean verifyCurrentEmailOtp(User user, String otp) {
        Optional<EmailChangeOtp> latestOtp =
                otpRepository.findTopByUserAndTypeOrderByExpiresAtDesc(user, "CURRENT");

        if (latestOtp.isEmpty()) return false;

        EmailChangeOtp entity = latestOtp.get();
        if (entity.isUsed() || entity.isExpired()) return false;
        if (!passwordEncoder.matches(otp, entity.getOtpHash())) return false;

        entity.setUsed(true);
        otpRepository.save(entity);

        log.info("✅ Verified CURRENT email OTP for user {}", user.getEmail());
        return true;
    }

    // -----------------------------------------------------
    // STEP 3️⃣  Send OTP to NEW email for confirmation
    // -----------------------------------------------------
    @Transactional
public void sendOtpToNewEmail(User user, String newEmail) {
    Optional<EmailChangeOtp> lastOtpOpt =
            otpRepository.findTopByUserAndTypeOrderByExpiresAtDesc(user, "NEW");

    if (lastOtpOpt.isPresent()) {
        EmailChangeOtp last = lastOtpOpt.get();
        if (!last.isExpired() && last.getCreatedAt() != null) {
            long secondsSinceLast = ChronoUnit.SECONDS.between(last.getCreatedAt(), Instant.now());
            if (secondsSinceLast < OTP_COOLDOWN_SECONDS) {
                long remaining = OTP_COOLDOWN_SECONDS - secondsSinceLast;

                // Format friendly time (seconds → mm:ss)
                String formattedWait = remaining < 60
                        ? remaining + " seconds"
                        : (remaining / 60) + " minute" + (remaining / 60 > 1 ? "s" : "");

                String msg = "Please wait " + formattedWait + " before requesting a new OTP.";

                log.warn("⏳ Cooldown active for NEW email OTP: {}s remaining for user {}", remaining, user.getId());
                throw new IllegalStateException(msg);
            }
        }
    }

    String otp = generateOtp();
    String hashedOtp = passwordEncoder.encode(otp);

    EmailChangeOtp otpEntity = EmailChangeOtp.create(
            user, hashedOtp, "NEW", newEmail
    );
    otpRepository.save(otpEntity);

    emailService.sendNewEmailConfirmationOtp(newEmail, otp);

    log.info("📨 Sent NEW email verification OTP to {}", newEmail);
}


    // -----------------------------------------------------
    // STEP 4️⃣  Verify OTP for NEW email & finalize change
    // -----------------------------------------------------
    @Transactional
    public boolean confirmEmailChange(User user, String newEmail, String otp) {
        Optional<EmailChangeOtp> latestOtp =
                otpRepository.findTopByUserAndTypeOrderByExpiresAtDesc(user, "NEW");

        if (latestOtp.isEmpty()) return false;

        EmailChangeOtp entity = latestOtp.get();

        if (entity.isUsed() || entity.isExpired()) return false;
        if (!entity.getNewEmail().equalsIgnoreCase(newEmail)) return false;
        if (!passwordEncoder.matches(otp, entity.getOtpHash())) return false;

        entity.setUsed(true);
        otpRepository.save(entity);

        user.setEmail(newEmail);
        userRepository.save(user);

        emailService.sendEmailChangeSuccessNotification(newEmail);

        log.info("✅ Email change completed for user {}, new email: {}", user.getId(), newEmail);
        return true;
    }

    // -----------------------------------------------------
    // 🔹 Generate Random 6-digit OTP
    // -----------------------------------------------------
    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    // -----------------------------------------------------
    // 🧹 Scheduled Cleanup Job — Runs Every Hour
    // -----------------------------------------------------
    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredOtps() {
        Instant now = Instant.now();
        int deleted = otpRepository.deleteAllByExpiresAtBeforeOrUsedTrue(now);
        if (deleted > 0) {
            log.info("🧹 Cleaned up {} expired/used OTP records at {}", deleted, now);
        }
    }
}

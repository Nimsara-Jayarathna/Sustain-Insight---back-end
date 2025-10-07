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
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailChangeService {

    private final EmailChangeOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // -----------------------------------------------------
    // STEP 1️⃣  Send OTP to CURRENT email for verification
    // -----------------------------------------------------
    @Transactional
    public void sendOtpToCurrentEmail(User user) {
        String otp = generateOtp();
        String hashedOtp = passwordEncoder.encode(otp);

        EmailChangeOtp otpEntity = EmailChangeOtp.create(
                user, hashedOtp, "CURRENT", null
        );
        otpRepository.save(otpEntity);

        // Use the new, clean method from EmailService
        emailService.sendCurrentEmailVerificationOtp(user.getEmail(), otp);
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
        return true;
    }

    // -----------------------------------------------------
    // STEP 3️⃣  Send OTP to NEW email for confirmation
    // -----------------------------------------------------
    @Transactional
    public void sendOtpToNewEmail(User user, String newEmail) {
        String otp = generateOtp();
        String hashedOtp = passwordEncoder.encode(otp);

        EmailChangeOtp otpEntity = EmailChangeOtp.create(
                user, hashedOtp, "NEW", newEmail
        );
        otpRepository.save(otpEntity);

        // Use the new, clean method from EmailService
        emailService.sendNewEmailConfirmationOtp(newEmail, otp);
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

        // ✅ Mark OTP as used
        entity.setUsed(true);
        otpRepository.save(entity);

        // ✅ Update user's email
        user.setEmail(newEmail);
        userRepository.save(user);

        // ✅ Notify user with the new, clean method
        emailService.sendEmailChangeSuccessNotification(newEmail);

        return true;
    }

    // -----------------------------------------------------
    // 🔹 Generate Random 6-digit OTP
    // -----------------------------------------------------
    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    // -----------------------------------------------------
    // 🧹 Scheduled Cleanup Job — Runs Every Hour
    // -----------------------------------------------------
    @Transactional
    @Scheduled(cron = "0 0 * * * *") // Every hour, on the hour
    public void cleanupExpiredOtps() {
        Instant now = Instant.now();
        int deleted = otpRepository.deleteAllByExpiresAtBeforeOrUsedTrue(now);

        if (deleted > 0) {
            log.info("🧹 Cleaned up {} expired/used OTP records at {}", deleted, now);
        }
    }
}

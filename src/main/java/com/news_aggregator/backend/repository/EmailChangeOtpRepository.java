package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.EmailChangeOtp;
import com.news_aggregator.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

public interface EmailChangeOtpRepository extends JpaRepository<EmailChangeOtp, Long> {
    Optional<EmailChangeOtp> findTopByUserAndTypeOrderByExpiresAtDesc(User user, String type);

    @Transactional
    int deleteAllByExpiresAtBeforeOrUsedTrue(Instant now);
}

package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.SourceFetchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SourceFetchLogRepository extends JpaRepository<SourceFetchLog, Long> {
    Optional<SourceFetchLog> findBySourceName(String sourceName);
}

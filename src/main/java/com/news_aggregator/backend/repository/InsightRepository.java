package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.Insight;
import com.news_aggregator.backend.model.InsightId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightRepository extends JpaRepository<Insight, InsightId> {
    boolean existsByUser_IdAndArticle_Id(Long userId, Long articleId);
    void deleteByUser_IdAndArticle_Id(Long userId, Long articleId);
    long countByArticle_Id(Long articleId);
}

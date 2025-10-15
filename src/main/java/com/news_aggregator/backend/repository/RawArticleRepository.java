package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.RawArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface RawArticleRepository extends JpaRepository<RawArticle, Long> {

    boolean existsByUrl(String url);

    boolean existsByTitleAndSourceName(String title, String sourceName);

    List<RawArticle> findByProcessedFalse();

    long countByProcessedFalse();

    // 🔹 NEW: Find the latest published article timestamp for a given source
    @Query("SELECT MAX(r.publishedAt) FROM RawArticle r WHERE r.apiSource = :source")
    OffsetDateTime findLatestPublishedAtBySource(@Param("source") String source);
}

package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.ArticleInsight;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ArticleInsightRepository extends JpaRepository<ArticleInsight, Long> {

    /**
     * 🔹 Get the cached insight count for a specific article.
     */
    @Query("SELECT ai.insightCount FROM ArticleInsight ai WHERE ai.articleId = :articleId")
    Long findCountByArticleId(Long articleId);

    /**
     * 🔹 Increment the cached insight count safely.
     * Uses PostgreSQL's ON CONFLICT for upsert behavior.
     */
    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO article_insights (article_id, insight_count)
        VALUES (:articleId, 1)
        ON CONFLICT (article_id)
        DO UPDATE SET insight_count = article_insights.insight_count + 1
        """, nativeQuery = true)
    void incrementCount(Long articleId);
}

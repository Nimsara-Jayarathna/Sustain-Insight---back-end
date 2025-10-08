package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    // ============================================================
    // 🔹 FETCHING / SORTING
    // ============================================================

    /**
     * Fetches the latest N articles ordered by publish date (DESC).
     * Used for home page highlights.
     */
    @Query(value = "SELECT * FROM articles ORDER BY published_at DESC LIMIT ?1", nativeQuery = true)
    List<Article> findTopNArticles(int limit);

    /**
     * Fetches a paginated list of articles ordered by popularity (insight count),
     * falling back to published_at DESC for ties.
     * Supports LIMIT + OFFSET for server-side pagination.
     */
    @Query(value = """
        SELECT a.*
        FROM articles a
        LEFT JOIN article_insights ai ON a.id = ai.article_id
        ORDER BY COALESCE(ai.insight_count, 0) DESC, a.published_at DESC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<Article> findAllOrderByPopularity(int limit, int offset);

    // ============================================================
    // 🔹 DUPLICATE VALIDATION
    // ============================================================

    /**
     * Checks if an article with the same title and summary already exists.
     */
    boolean existsByTitleAndSummary(String title, String summary);

    /**
     * Checks if an article with the same title already exists.
     */
    boolean existsByTitle(String title);

    /**
     * Checks for duplicates by (title + publishedAt + source name).
     * Used for precise duplicate detection during ingestion.
     */
    boolean existsByTitleAndPublishedAtAndSources_Name(
            String title,
            OffsetDateTime publishedAt,
            String sourceName
    );
}

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

    // 🔹 Fetch latest N articles (Postgres-friendly)
    @Query(value = "SELECT * FROM articles ORDER BY published_at DESC LIMIT ?1", nativeQuery = true)
    List<Article> findTopNArticles(int limit);

    // 🔹 Duplicate check (simple)
    boolean existsByTitleAndSummary(String title, String summary);

    // 🔹 Duplicate check (title only)
    boolean existsByTitle(String title);

    // 🔹 Duplicate check (precise: title + publishedAt + source name)
    boolean existsByTitleAndPublishedAtAndSources_Name(
            String title,
            OffsetDateTime publishedAt,
            String sourceName
    );
}

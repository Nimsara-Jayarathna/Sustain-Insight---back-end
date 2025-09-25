package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    // Native Postgres-friendly query to fetch latest N articles
    @Query(value = "SELECT * FROM articles ORDER BY published_at DESC LIMIT ?1", nativeQuery = true)
    List<Article> findTopNArticles(int limit);

}

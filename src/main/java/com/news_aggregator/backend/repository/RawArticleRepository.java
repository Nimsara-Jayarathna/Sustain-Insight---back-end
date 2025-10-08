package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.RawArticle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawArticleRepository extends JpaRepository<RawArticle, Long> {
    boolean existsByUrl(String url);
    boolean existsByTitleAndSourceName(String title, String sourceName);
}

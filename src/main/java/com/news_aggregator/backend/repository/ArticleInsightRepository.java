package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.ArticleInsight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleInsightRepository extends JpaRepository<ArticleInsight, Long> {
}

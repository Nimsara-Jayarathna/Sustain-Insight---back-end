package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.Insight;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.ArticleRepository;
import com.news_aggregator.backend.repository.InsightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Handles creation, removal, and tracking of article insights.
 * Maintains a running count on the primary articles table for faster reads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private final InsightRepository insightRepository;
    private final ArticleRepository articleRepository;

    /**
     * 🔹 Adds a new insight (view/action) for a given user and article.
     * Ensures no duplicates and updates cached count atomically.
     */
    @Transactional
    public void addInsight(User user, Article article) {
        if (article == null || article.getId() == null || user == null) return;

        // Skip duplicate insights
        if (insightRepository.existsByUser_IdAndArticle_Id(user.getId(), article.getId())) {
            log.debug("Insight already exists for user {} and article {}", user.getId(), article.getId());
            return;
        }

        // Save new insight record (user_id + article_id composite PK)
        Insight insight = Insight.builder()
                .user(user)
                .article(article)
                .createdAt(OffsetDateTime.now())
                .build();
        insightRepository.save(insight);

        // Increment cached count (stores directly on articles table)
        adjustInsightCount(article, +1);
        log.debug("Insight added: article={}, user={}", article.getId(), user.getId());
    }

    /**
     * 🔹 Removes an existing insight and decrements cached count.
     */
    @Transactional
    public void removeInsight(User user, Article article) {
        if (article == null || article.getId() == null || user == null) return;

        if (insightRepository.existsByUser_IdAndArticle_Id(user.getId(), article.getId())) {
            insightRepository.deleteByUser_IdAndArticle_Id(user.getId(), article.getId());
            adjustInsightCount(article, -1);
            log.debug("Insight removed: article={}, user={}", article.getId(), user.getId());
        }
    }

    /**
     * 🔹 Checks if a user has already viewed/insighted an article.
     */
    public boolean isInsighted(User user, Article article) {
        if (article == null || article.getId() == null || user == null) return false;
        return insightRepository.existsByUser_IdAndArticle_Id(user.getId(), article.getId());
    }

    /**
     * 🔹 Returns total cached insights for an article.
     */
    public long getCount(Article article) {
        if (article == null || article.getId() == null) return 0L;
        Long cached = article.getInsightCount();
        if (cached != null) {
            return cached;
        }

        Long persisted = articleRepository.findInsightCountById(article.getId());
        return persisted != null ? persisted : 0L;
    }

    /**
     * 🔹 Adjusts cached count in the articles table.
     * Ensures count never drops below zero.
     */
    private void adjustInsightCount(Article article, int delta) {
        if (delta > 0) {
            articleRepository.incrementInsightCount(article.getId());
            long newCount = article.getInsightCountOrZero() + delta;
            article.setInsightCount(newCount);
        } else if (delta < 0) {
            articleRepository.decrementInsightCount(article.getId());
            long current = article.getInsightCountOrZero();
            article.setInsightCount(Math.max(0, current + delta));
        }
    }
}

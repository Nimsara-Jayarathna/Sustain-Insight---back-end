package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.ArticleInsight;
import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.Insight;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.ArticleInsightRepository;
import com.news_aggregator.backend.repository.InsightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final InsightRepository insightRepository;
    private final ArticleInsightRepository articleInsightRepository;

    @Transactional
    public void addInsight(User user, Article article) {
        if (!insightRepository.existsByUser_IdAndArticle_Id(user.getId(), article.getId())) {
            Insight insight = Insight.builder()
                    .user(user)
                    .article(article)
                    .createdAt(OffsetDateTime.now())
                    .build();
            insightRepository.save(insight);
            updateCount(article.getId(), +1);
        }
    }

    @Transactional
    public void removeInsight(User user, Article article) {
        if (insightRepository.existsByUser_IdAndArticle_Id(user.getId(), article.getId())) {
            insightRepository.deleteByUser_IdAndArticle_Id(user.getId(), article.getId());
            updateCount(article.getId(), -1);
        }
    }

    public boolean isInsighted(User user, Article article) {
        return insightRepository.existsByUser_IdAndArticle_Id(user.getId(), article.getId());
    }

    public long getCount(Article article) {
        return articleInsightRepository.findById(article.getId())
                .map(ArticleInsight::getInsightCount)
                .orElse(0L);
    }

    private void updateCount(Long articleId, int delta) {
        ArticleInsight ai = articleInsightRepository.findById(articleId)
                .orElseGet(() -> new ArticleInsight(articleId, 0L));
        long newCount = ai.getInsightCount() + delta;
        ai.setInsightCount(Math.max(0, newCount));
        articleInsightRepository.save(ai);
    }
}

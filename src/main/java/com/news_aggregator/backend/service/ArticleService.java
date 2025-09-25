package com.news_aggregator.backend.service;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.Category;
import com.news_aggregator.backend.model.Source;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.ArticleRepository;
import com.news_aggregator.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Autowired
    public ArticleService(ArticleRepository articleRepository, UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public List<ArticleDto> getLatestArticles(int limit) {
        List<Article> articles = articleRepository.findTopNArticles(limit);
        return articles.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ArticleDto> getAllArticles(List<Long> categoryIds, List<Long> sourceIds, String keyword, LocalDate date) {
        System.out.println("Filtering with keyword: " + keyword);
        System.out.println("Filtering with categories: " + categoryIds);
        System.out.println("Filtering with sources: " + sourceIds);
        System.out.println("Filtering with date: " + date);

        Specification<Article> spec = Specification.where(ArticleSpecification.hasKeyword(keyword))
                .and(ArticleSpecification.hasCategories(categoryIds))
                .and(ArticleSpecification.hasSources(sourceIds))
                .and(ArticleSpecification.hasDate(date));

        List<Article> articles = articleRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "publishedAt"));

        return articles.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ArticleDto> getForYouFeed(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Long> preferredCategoryIds = user.getPreferredCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toList());
        List<Long> preferredSourceIds = user.getPreferredSources().stream()
                .map(Source::getId)
                .collect(Collectors.toList());

        // If user has no preferences, return an empty list
        if (preferredCategoryIds.isEmpty() && preferredSourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        OffsetDateTime twentyFourHoursAgo = OffsetDateTime.now().minus(24, ChronoUnit.HOURS);
        System.out.println("DEBUG: twentyFourHoursAgo = " + twentyFourHoursAgo);

        Specification<Article> spec = Specification.where(ArticleSpecification.hasCategories(preferredCategoryIds))
                .and(ArticleSpecification.hasSources(preferredSourceIds))
                .and(ArticleSpecification.isWithinLast24Hours());

        List<Article> articles = articleRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "publishedAt"));

        return articles.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ArticleDto mapToDto(Article article) {
        return new ArticleDto(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getImageUrl(),
                article.getPublishedAt(),
                article.getSources().stream().map(Source::getName).collect(Collectors.toList()),
                article.getCategories().stream().map(Category::getName).collect(Collectors.toList())
        );
    }
}

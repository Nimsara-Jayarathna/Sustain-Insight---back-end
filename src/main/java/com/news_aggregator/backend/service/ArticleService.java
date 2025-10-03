package com.news_aggregator.backend.service;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.dto.PagedResponse;
import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.Category;
import com.news_aggregator.backend.model.Source;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.ArticleRepository;
import com.news_aggregator.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Value("${feed.hoursWindow}")   // 👈 configurable via env
    private int feedHoursWindow;

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

    // Legacy non-paginated
    public List<ArticleDto> getAllArticles(List<Long> categoryIds, List<Long> sourceIds, String keyword, LocalDate date) {
        Specification<Article> spec = Specification.where(ArticleSpecification.hasKeyword(keyword))
                .and(ArticleSpecification.hasCategories(categoryIds))
                .and(ArticleSpecification.hasSources(sourceIds))
                .and(ArticleSpecification.hasDate(date));

        List<Article> articles = articleRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return articles.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Legacy non-paginated feed
    public List<ArticleDto> getForYouFeed(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Long> preferredCategoryIds = user.getPreferredCategories().stream().map(Category::getId).toList();
        List<Long> preferredSourceIds = user.getPreferredSources().stream().map(Source::getId).toList();

        if (preferredCategoryIds.isEmpty() && preferredSourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        Specification<Article> spec = Specification.where(ArticleSpecification.hasCategories(preferredCategoryIds))
                .and(ArticleSpecification.hasSources(preferredSourceIds))
                .and(ArticleSpecification.publishedWithinLastHours(feedHoursWindow)); // 👈 uses env-based hours

        List<Article> articles = articleRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return articles.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Pagination All Articles
    public PagedResponse<ArticleDto> getAllArticles(List<Long> categoryIds,
                                                    List<Long> sourceIds,
                                                    String keyword,
                                                    LocalDate date,
                                                    int page,
                                                    int size) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Specification<Article> spec = Specification.where(ArticleSpecification.hasKeyword(keyword))
                .and(ArticleSpecification.hasCategories(categoryIds))
                .and(ArticleSpecification.hasSources(sourceIds))
                .and(ArticleSpecification.hasDate(date));

        Page<Article> articlePage = articleRepository.findAll(spec, pageable);
        List<ArticleDto> dtos = articlePage.getContent().stream().map(this::mapToDto).toList();

        return new PagedResponse<>(
                dtos,
                articlePage.getNumber() + 1,
                articlePage.getTotalPages(),
                articlePage.getTotalElements(),
                articlePage.getSize()
        );
    }

    // Pagination For You Feed
    public PagedResponse<ArticleDto> getForYouFeed(UserDetails userDetails, int page, int size) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Long> preferredCategoryIds = user.getPreferredCategories().stream().map(Category::getId).toList();
        List<Long> preferredSourceIds = user.getPreferredSources().stream().map(Source::getId).toList();

        if (preferredCategoryIds.isEmpty() && preferredSourceIds.isEmpty()) {
            return new PagedResponse<>(Collections.emptyList(), page, 0, 0, size);
        }

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Specification<Article> spec = Specification.where(ArticleSpecification.hasCategories(preferredCategoryIds))
                .and(ArticleSpecification.hasSources(preferredSourceIds))
                .and(ArticleSpecification.publishedWithinLastHours(feedHoursWindow)); // 👈 uses env-based hours

        Page<Article> articlePage = articleRepository.findAll(spec, pageable);
        List<ArticleDto> dtos = articlePage.getContent().stream().map(this::mapToDto).toList();

        return new PagedResponse<>(
                dtos,
                articlePage.getNumber() + 1,
                articlePage.getTotalPages(),
                articlePage.getTotalElements(),
                articlePage.getSize()
        );
    }

    private ArticleDto mapToDto(Article article) {
        return new ArticleDto(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getImageUrl(),
                article.getPublishedAt(),
                article.getSources().stream().map(Source::getName).toList(),
                article.getCategories().stream().map(Category::getName).toList()
        );
    }
}

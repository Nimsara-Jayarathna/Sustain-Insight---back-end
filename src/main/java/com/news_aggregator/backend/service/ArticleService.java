package com.news_aggregator.backend.service;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.dto.PagedResponse;
import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.Category;
import com.news_aggregator.backend.model.Source;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.ArticleRepository;
import com.news_aggregator.backend.repository.BookmarkRepository;
import com.news_aggregator.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final InsightService insightService;

    @Value("${feed.hoursWindow}") // configurable via env
    private int feedHoursWindow;

    // 🔹 Latest Articles (no pagination, no user context)
    public List<ArticleDto> getLatestArticles(int limit) {
        List<Article> articles = articleRepository.findTopNArticles(limit);
        return articles.stream()
                .map(article -> mapToDto(article, null))
                .collect(Collectors.toList());
    }

    // 🔹 All Articles (public, supports sorting)
    public PagedResponse<ArticleDto> getAllArticles(
            List<Long> categoryIds,
            List<Long> sourceIds,
            String keyword,
            LocalDate date,
            int page,
            int size,
            String sortParam
    ) {
        return getAllArticles(categoryIds, sourceIds, keyword, date, page, size, sortParam, null);
    }

    // 🔹 All Articles (authenticated, supports sorting + bookmarks + insights)
    public PagedResponse<ArticleDto> getAllArticles(
            List<Long> categoryIds,
            List<Long> sourceIds,
            String keyword,
            LocalDate date,
            int page,
            int size,
            String sortParam,
            UserDetails userDetails
    ) {
        User user = userDetails != null ? getUser(userDetails) : null;

        // ✅ Handle "popular" sort using cached insight counts
        if ("popular".equalsIgnoreCase(sortParam)) {
    Specification<Article> spec = Specification.where(ArticleSpecification.hasKeyword(keyword))
            .and(ArticleSpecification.hasCategories(categoryIds))
            .and(ArticleSpecification.hasSources(sourceIds))
            .and(ArticleSpecification.hasDate(date));

    // Fetch all filtered results first (within reason)
    List<Article> filtered = articleRepository.findAll(spec);

    // Sort by insight_count (cached) DESC, then publishedAt DESC
    filtered.sort((a, b) -> {
        long countA = insightService.getCount(a);
        long countB = insightService.getCount(b);

        if (countA != countB)
            return Long.compare(countB, countA); // higher first

        // tie-breaker: newer first
        if (a.getPublishedAt() == null && b.getPublishedAt() == null) return 0;
        if (a.getPublishedAt() == null) return 1;
        if (b.getPublishedAt() == null) return -1;
        return b.getPublishedAt().compareTo(a.getPublishedAt());
    });

    // Pagination manually
    int fromIndex = Math.min((page - 1) * size, filtered.size());
    int toIndex = Math.min(fromIndex + size, filtered.size());
    List<Article> paginated = filtered.subList(fromIndex, toIndex);

    List<ArticleDto> dtos = paginated.stream()
            .map(article -> mapToDto(article, user))
            .toList();

    return new PagedResponse<>(
            dtos,
            page,
            size,
            filtered.size(),
            (int) Math.ceil((double) filtered.size() / size),
            toIndex >= filtered.size()
    );
}


        // ✅ Default sort handling (newest or oldest)
        Sort sort = switch (sortParam == null ? "newest" : sortParam.toLowerCase()) {
            case "oldest" -> Sort.by(Sort.Order.asc("publishedAt"));
            default -> Sort.by(Sort.Order.desc("publishedAt"));
        };

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, sort);
        Specification<Article> spec = Specification.where(ArticleSpecification.hasKeyword(keyword))
                .and(ArticleSpecification.hasCategories(categoryIds))
                .and(ArticleSpecification.hasSources(sourceIds))
                .and(ArticleSpecification.hasDate(date));

        Page<Article> articlePage = articleRepository.findAll(spec, pageable);

        List<ArticleDto> dtos = articlePage.getContent().stream()
                .map(article -> mapToDto(article, user))
                .toList();

        return new PagedResponse<>(
                dtos,
                articlePage.getNumber() + 1, // convert to 1-based index
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages(),
                articlePage.isLast()
        );
    }

    // 🔹 Personalized Feed (non-paginated)
    public List<ArticleDto> getForYouFeed(UserDetails userDetails) {
        User user = getUser(userDetails);

        List<Long> preferredCategoryIds = user.getPreferredCategories().stream()
                .map(Category::getId)
                .toList();
        List<Long> preferredSourceIds = user.getPreferredSources().stream()
                .map(Source::getId)
                .toList();

        if (preferredCategoryIds.isEmpty() && preferredSourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        Specification<Article> spec = Specification.where(ArticleSpecification.hasCategories(preferredCategoryIds))
                .and(ArticleSpecification.hasSources(preferredSourceIds))
                .and(ArticleSpecification.publishedWithinLastHours(feedHoursWindow));

        List<Article> articles = articleRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "publishedAt"));

        return articles.stream()
                .map(article -> mapToDto(article, user))
                .collect(Collectors.toList());
    }

    // 🔹 Personalized Feed (paginated)
    public PagedResponse<ArticleDto> getForYouFeed(UserDetails userDetails, int page, int size) {
        User user = getUser(userDetails);

        List<Long> preferredCategoryIds = user.getPreferredCategories().stream()
                .map(Category::getId)
                .toList();
        List<Long> preferredSourceIds = user.getPreferredSources().stream()
                .map(Source::getId)
                .toList();

        if (preferredCategoryIds.isEmpty() && preferredSourceIds.isEmpty()) {
            return new PagedResponse<>(Collections.emptyList(), page, size, 0, 0, true);
        }

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Specification<Article> spec = Specification.where(ArticleSpecification.hasCategories(preferredCategoryIds))
                .and(ArticleSpecification.hasSources(preferredSourceIds))
                .and(ArticleSpecification.publishedWithinLastHours(feedHoursWindow));

        Page<Article> articlePage = articleRepository.findAll(spec, pageable);

        List<ArticleDto> dtos = articlePage.getContent().stream()
                .map(article -> mapToDto(article, user))
                .toList();

        return new PagedResponse<>(
                dtos,
                articlePage.getNumber() + 1,
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages(),
                articlePage.isLast()
        );
    }

    // --- 🔹 Helpers ---

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * 🔹 Maps Article → ArticleDto with bookmark and insight metadata.
     */
    public ArticleDto mapToDto(Article article, User user) {
        boolean bookmarked = false;
        boolean insighted = false;
        long insightCount = insightService.getCount(article); // cached count

        if (user != null) {
            bookmarked = bookmarkRepository.existsByUser_IdAndArticle_Id(user.getId(), article.getId());
            insighted = insightService.isInsighted(user, article);
        }

        return new ArticleDto(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getImageUrl(),
                article.getPublishedAt(),
                article.getSources().stream().map(Source::getName).toList(),
                article.getCategories().stream().map(Category::getName).toList(),
                bookmarked,
                insighted,
                insightCount
        );
    }

    public Article getArticleEntity(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));
    }
}

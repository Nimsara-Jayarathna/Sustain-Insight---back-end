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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;

    @Value("${feed.hoursWindow}")   // configurable via env
    private int feedHoursWindow;

    @Autowired
    public ArticleService(ArticleRepository articleRepository,
                          UserRepository userRepository,
                          BookmarkRepository bookmarkRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    // 🔹 Latest Articles (no pagination, no user context)
    public List<ArticleDto> getLatestArticles(int limit) {
        List<Article> articles = articleRepository.findTopNArticles(limit);
        return articles.stream()
                .map(article -> mapToDto(article, false)) // no user = no bookmark state
                .collect(Collectors.toList());
    }

    public PagedResponse<ArticleDto> getAllArticles(
        List<Long> categoryIds,
        List<Long> sourceIds,
        String keyword,
        LocalDate date,
        int page,
        int size
) {
    Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
    Specification<Article> spec = Specification.where(ArticleSpecification.hasKeyword(keyword))
            .and(ArticleSpecification.hasCategories(categoryIds))
            .and(ArticleSpecification.hasSources(sourceIds))
            .and(ArticleSpecification.hasDate(date));

    Page<Article> articlePage = articleRepository.findAll(spec, pageable);

    List<ArticleDto> dtos = articlePage.getContent().stream()
            .map(article -> mapToDto(article, false))
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


    // 🔹 Legacy non-paginated feed (user-specific)
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
                .and(ArticleSpecification.publishedWithinLastHours(feedHoursWindow));

        List<Article> articles = articleRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "publishedAt"));

        // fetch bookmarked IDs for this user
        Set<Long> bookmarkedIds = bookmarkRepository.findAllByUserId(user.getId())
                .stream()
                .map(b -> b.getArticle().getId())
                .collect(Collectors.toSet());

        return articles.stream()
                .map(article -> mapToDto(article, bookmarkedIds.contains(article.getId())))
                .collect(Collectors.toList());
    }

    // 🔹 Pagination: All Articles
    public PagedResponse<ArticleDto> getAllArticles(List<Long> categoryIds,
                                                    List<Long> sourceIds,
                                                    String keyword,
                                                    LocalDate date,
                                                    int page,
                                                    int size,
                                                    UserDetails userDetails) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Specification<Article> spec = Specification.where(ArticleSpecification.hasKeyword(keyword))
                .and(ArticleSpecification.hasCategories(categoryIds))
                .and(ArticleSpecification.hasSources(sourceIds))
                .and(ArticleSpecification.hasDate(date));

        Page<Article> articlePage = articleRepository.findAll(spec, pageable);

        // check bookmarks if user logged in
        Set<Long> bookmarkedIds = getBookmarkedIds(userDetails);

        List<ArticleDto> dtos = articlePage.getContent().stream()
                .map(article -> mapToDto(article, bookmarkedIds.contains(article.getId())))
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

    // 🔹 Pagination: For You Feed
    public PagedResponse<ArticleDto> getForYouFeed(UserDetails userDetails, int page, int size) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Long> preferredCategoryIds = user.getPreferredCategories().stream().map(Category::getId).toList();
        List<Long> preferredSourceIds = user.getPreferredSources().stream().map(Source::getId).toList();

        if (preferredCategoryIds.isEmpty() && preferredSourceIds.isEmpty()) {
            return new PagedResponse<>(Collections.emptyList(), page, size, 0, 0, true);
        }

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Specification<Article> spec = Specification.where(ArticleSpecification.hasCategories(preferredCategoryIds))
                .and(ArticleSpecification.hasSources(preferredSourceIds))
                .and(ArticleSpecification.publishedWithinLastHours(feedHoursWindow));

        Page<Article> articlePage = articleRepository.findAll(spec, pageable);

        Set<Long> bookmarkedIds = getBookmarkedIds(userDetails);

        List<ArticleDto> dtos = articlePage.getContent().stream()
                .map(article -> mapToDto(article, bookmarkedIds.contains(article.getId())))
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

    // --- Helpers ---

    private Set<Long> getBookmarkedIds(UserDetails userDetails) {
        if (userDetails == null) {
            return Collections.emptySet();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return bookmarkRepository.findAllByUserId(user.getId())
                .stream()
                .map(b -> b.getArticle().getId())
                .collect(Collectors.toSet());
    }

    private ArticleDto mapToDto(Article article, boolean bookmarked) {
        return new ArticleDto(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getImageUrl(),
                article.getPublishedAt(),
                article.getSources().stream().map(Source::getName).toList(),
                article.getCategories().stream().map(Category::getName).toList(),
                bookmarked
        );
    }
}

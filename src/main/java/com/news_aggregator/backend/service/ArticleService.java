package com.news_aggregator.backend.service;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.dto.PagedResponse;
import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.Category;
import com.news_aggregator.backend.model.Source;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final InsightService insightService;
    private final CategoryRepository categoryRepository;
    private final SourceRepository sourceRepository;
    private final RawArticleRepository rawArticleRepository;
    private final CategoryService categoryService;
    private final SourceService sourceService;

    @Value("${feed.hoursWindow}")
    private int feedHoursWindow;

    @Transactional(readOnly = true)
    public List<ArticleDto> getLatestArticles(int limit) {
        List<Article> articles = articleRepository.findTopNArticles(limit);
        return articles.stream()
                .map(article -> mapToDto(article, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

        if ("popular".equalsIgnoreCase(sortParam)) {
            Specification<Article> spec = Specification.allOf(
                    ArticleSpecification.hasKeyword(keyword),
                    ArticleSpecification.hasCategories(categoryIds),
                    ArticleSpecification.hasSources(sourceIds),
                    ArticleSpecification.hasDate(date)
            );

            List<Article> filtered = articleRepository.findAll(spec);

            filtered.sort((a, b) -> {
                long countA = insightService.getCount(a);
                long countB = insightService.getCount(b);
                if (countA != countB) return Long.compare(countB, countA);
                if (a.getPublishedAt() == null && b.getPublishedAt() == null) return 0;
                if (a.getPublishedAt() == null) return 1;
                if (b.getPublishedAt() == null) return -1;
                return b.getPublishedAt().compareTo(a.getPublishedAt());
            });

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

        Sort sort = switch (sortParam == null ? "newest" : sortParam.toLowerCase()) {
            case "oldest" -> Sort.by(Sort.Order.asc("publishedAt"));
            default -> Sort.by(Sort.Order.desc("publishedAt"));
        };

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, sort);
        Specification<Article> spec = Specification.allOf(
                ArticleSpecification.hasKeyword(keyword),
                ArticleSpecification.hasCategories(categoryIds),
                ArticleSpecification.hasSources(sourceIds),
                ArticleSpecification.hasDate(date)
        );

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

    @Transactional(readOnly = true)
    public List<ArticleDto> getForYouFeed(UserDetails userDetails) {
        User user = getUser(userDetails);
        List<Long> preferredCategoryIds = user.getPreferredCategories().stream().map(Category::getId).toList();
        List<Long> preferredSourceIds = user.getPreferredSources().stream().map(Source::getId).toList();

        if (preferredCategoryIds.isEmpty() && preferredSourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        Specification<Article> spec = Specification.allOf(
                ArticleSpecification.hasCategories(preferredCategoryIds),
                ArticleSpecification.hasSources(preferredSourceIds),
                ArticleSpecification.publishedWithinLastHours(feedHoursWindow)
        );

        List<Article> articles = articleRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "publishedAt"));

        return articles.stream()
                .map(article -> mapToDto(article, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ArticleDto> getForYouFeed(UserDetails userDetails, int page, int size) {
        User user = getUser(userDetails);
        List<Long> preferredCategoryIds = user.getPreferredCategories().stream().map(Category::getId).toList();
        List<Long> preferredSourceIds = user.getPreferredSources().stream().map(Source::getId).toList();

        if (preferredCategoryIds.isEmpty() && preferredSourceIds.isEmpty()) {
            return new PagedResponse<>(Collections.emptyList(), page, size, 0, 0, true);
        }

        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Specification<Article> spec = Specification.allOf(
                ArticleSpecification.hasCategories(preferredCategoryIds),
                ArticleSpecification.hasSources(preferredSourceIds),
                ArticleSpecification.publishedWithinLastHours(feedHoursWindow)
        );

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

    private User getUser(UserDetails userDetails) {
        return userRepository.findProfileByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public ArticleDto mapToDto(Article article, User user) {
        boolean bookmarked = false;
        boolean insighted = false;
        long insightCount = insightService.getCount(article);

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

    /**
     * Loads and maps an article while the persistence context is still open.
     * This is the safe boundary for callers that need a DTO outside this service,
     * because Article.sources and Article.categories are intentionally LAZY.
     */
    @Transactional(readOnly = true)
    public ArticleDto getArticleDto(Long articleId, User user) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));
        return mapToDto(article, user);
    }

    @Transactional(readOnly = true)
    public Article getArticleEntity(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));
    }

    @Transactional
    public void saveSynthesizedArticles(List<Map<String, Object>> articles) {
        for (Map<String, Object> articleMap : articles) {
            Article article = new Article();
            article.setTitle((String) articleMap.get("title"));
            article.setSummary((String) articleMap.get("summary"));
            article.setContent((String) articleMap.get("content"));
            article.setImageUrl((String) articleMap.get("image_url"));

            try {
                String publishedAtStr = (String) articleMap.get("published_at");
                if (publishedAtStr != null) {
                    article.setPublishedAt(OffsetDateTime.parse(publishedAtStr));
                } else {
                    article.setPublishedAt(OffsetDateTime.now());
                }
            } catch (Exception e) {
                System.err.println("⚠️ Could not parse date: " + articleMap.get("published_at") + ". Defaulting to now.");
                article.setPublishedAt(OffsetDateTime.now());
            }

            List<Long> categoryIds = extractLongIds(articleMap.get("category_ids"));
            List<Category> categories = categoryIds.isEmpty()
                    ? List.of()
                    : categoryRepository.findAllById(categoryIds);
            if (categories.isEmpty()) {
                categories = List.of(categoryService.getDefaultCategory());
            }
            article.setCategories(new ArrayList<>(categories));

            List<Long> sourceIds = extractLongIds(articleMap.get("source_ids"));
            List<Source> sources = sourceIds.isEmpty()
                    ? List.of()
                    : sourceRepository.findAllById(sourceIds);

            if (sources.isEmpty()) {
                Source fallbackSource = resolveFallbackSource(articleMap);
                if (fallbackSource != null) {
                    sources = List.of(fallbackSource);
                }
            }
            article.setSources(new ArrayList<>(sources));

            articleRepository.save(article);
        }
    }
    private List<Long> extractLongIds(Object rawIds) {
        if (!(rawIds instanceof List<?> ids)) {
            return List.of();
        }

        return ids.stream()
                .map(value -> {
                    if (value instanceof Number number) {
                        return number.longValue();
                    }
                    try {
                        return value == null ? null : Long.valueOf(value.toString());
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Source resolveFallbackSource(Map<String, Object> articleMap) {
        Long rawArticleId = null;
        Object idValue = articleMap.get("id");
        if (idValue instanceof Number number) {
            rawArticleId = number.longValue();
        } else if (idValue != null) {
            try {
                rawArticleId = Long.valueOf(idValue.toString());
            } catch (NumberFormatException ignored) {
                // Fall through to provider-level fallback below.
            }
        }

        if (rawArticleId != null) {
            var rawArticle = rawArticleRepository.findById(rawArticleId).orElse(null);
            if (rawArticle != null) {
                String candidate = rawArticle.getSourceName();
                if (candidate == null || candidate.isBlank()) {
                    candidate = rawArticle.getApiSource();
                }
                if (candidate != null && !candidate.isBlank()) {
                    return sourceService.getOrCreate(candidate);
                }
            }
        }

        Object provider = articleMap.get("api_source");
        if (provider != null && !provider.toString().isBlank()) {
            return sourceService.getOrCreate(provider.toString());
        }
        return null;
    }

}

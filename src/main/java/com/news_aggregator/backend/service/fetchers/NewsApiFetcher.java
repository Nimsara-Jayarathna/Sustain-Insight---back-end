package com.news_aggregator.backend.service.fetchers;

import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.repository.RawArticleRepository;
import com.news_aggregator.backend.service.filters.EsgFilterService;
import com.news_aggregator.backend.service.filters.TextNormalizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsApiFetcher implements RawNewsSourceFetcher {

    private static final String BASIC_QUERY =
            "(ESG OR sustainability OR sustainable OR climate OR renewable OR green OR environment OR carbon OR \"net zero\")";

    private final RestTemplate restTemplate;
    private final RawArticleRepository rawRepo;
    private final EsgFilterService filter;
    private final TextNormalizerService normalizer;

    @Value("${newsapi.url}")
    private String baseUrl;

    @Value("${newsapi.apiKey}")
    private String apiKey;

    @Value("${newsapi.language:en}")
    private String language;

    @Value("${newsapi.pageSize:50}")
    private int pageSize;

    @Value("${newsapi.maxPages:2}")
    private int maxPages;

    @Value("${newsapi.pageDelayMs:2000}")
    private long pageDelayMs;

    @Override
    public String getSourceName() {
        return "NewsAPI";
    }

    @Override
    public List<RawArticle> fetchArticles(int limit) {
        List<RawArticle> savedArticles = new ArrayList<>();
        int savedCount = 0;
        int duplicateCount = 0;
        int skippedCount = 0;

        int effectivePageSize = Math.max(1, Math.min(pageSize, 100));
        int effectiveMaxPages = Math.max(1, maxPages);

        for (int page = 1; page <= effectiveMaxPages; page++) {
            int savedBeforePage = savedCount;
            int duplicatesBeforePage = duplicateCount;
            int skippedBeforePage = skippedCount;

            try {
                String url = UriComponentsBuilder.fromUriString(baseUrl)
                        .path("/everything")
                        .queryParam("q", BASIC_QUERY)
                        .queryParam("language", language)
                        .queryParam("pageSize", effectivePageSize)
                        .queryParam("page", page)
                        .queryParam("apiKey", apiKey)
                        .build()
                        .encode()
                        .toUriString();

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    log.warn("[NewsAPI] Non-success response on page {}: {}", page, response.getStatusCode());
                    break;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> articles =
                        (List<Map<String, Object>>) response.getBody().get("articles");

                if (articles == null || articles.isEmpty()) {
                    log.info("[NewsAPI] No more articles returned at page {}. Stopping pagination.", page);
                    break;
                }

                for (Map<String, Object> item : articles) {
                    try {
                        String title = normalizer.normalize((String) item.get("title"));
                        String description = normalizer.normalize((String) item.get("description"));
                        String content = normalizer.normalize((String) item.get("content"));
                        String articleUrl = (String) item.get("url");
                        String sourceName = extractSourceName(item.get("source"));

                        if (articleUrl != null && rawRepo.existsByUrl(articleUrl)) {
                            duplicateCount++;
                            continue;
                        }

                        if (title != null && sourceName != null
                                && rawRepo.existsByTitleAndSourceName(title, sourceName)) {
                            duplicateCount++;
                            continue;
                        }

                        if (!filter.isEsgRelevant(title, description, content)) {
                            skippedCount++;
                            continue;
                        }

                        RawArticle raw = new RawArticle();
                        raw.setApiSource(getSourceName());
                        raw.setTitle(title);
                        raw.setDescription(description);
                        raw.setContent(content);
                        raw.setUrl(articleUrl);
                        raw.setImageUrl((String) item.get("urlToImage"));
                        raw.setSourceName(sourceName);

                        String publishedAt = (String) item.get("publishedAt");
                        if (publishedAt != null && !publishedAt.isBlank()) {
                            raw.setPublishedAt(OffsetDateTime.parse(publishedAt));
                        }

                        raw.setRawJson(item);
                        rawRepo.save(raw);
                        savedArticles.add(raw);
                        savedCount++;

                        if (limit > 0 && savedCount >= limit) {
                            logPageSummary(page, savedCount - savedBeforePage,
                                    duplicateCount - duplicatesBeforePage,
                                    skippedCount - skippedBeforePage);
                            log.info("[NewsAPI] Requested save limit {} reached. Stopping pagination.", limit);
                            return finish(savedArticles, savedCount, duplicateCount, skippedCount);
                        }
                    } catch (Exception itemException) {
                        skippedCount++;
                        log.warn("[NewsAPI] Skipping malformed/unpersistable article on page {}: {}",
                                page, itemException.getMessage());
                    }
                }

                logPageSummary(page, savedCount - savedBeforePage,
                        duplicateCount - duplicatesBeforePage,
                        skippedCount - skippedBeforePage);

                if (articles.size() < effectivePageSize) {
                    log.info("[NewsAPI] Last page reached at page {} ({} < {}).",
                            page, articles.size(), effectivePageSize);
                    break;
                }

                if (!sleepBetweenPages(page, effectiveMaxPages)) {
                    break;
                }
            } catch (RestClientResponseException responseException) {
                if (isDeveloperResultLimit(responseException)) {
                    log.warn("[NewsAPI] Developer-plan result limit reached on page {}. Stopping pagination.", page);
                    break;
                }

                log.warn("[NewsAPI] HTTP error on page {}: status={} body={}",
                        page,
                        responseException.getStatusCode().value(),
                        safeBody(responseException.getResponseBodyAsString()));
                break;
            } catch (Exception exception) {
                log.warn("[NewsAPI] Fetch failed on page {}: {}", page, exception.getMessage());
                break;
            }
        }

        return finish(savedArticles, savedCount, duplicateCount, skippedCount);
    }

    private String extractSourceName(Object source) {
        if (source instanceof Map<?, ?> sourceMap) {
            Object name = sourceMap.get("name");
            return name instanceof String value ? value : null;
        }
        return null;
    }

    private boolean isDeveloperResultLimit(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        return exception.getStatusCode().value() == 426
                && body != null
                && body.contains("maximumResultsReached");
    }

    private String safeBody(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        return body.length() <= 500 ? body : body.substring(0, 500) + "...";
    }

    private boolean sleepBetweenPages(int currentPage, int effectiveMaxPages) {
        if (currentPage >= effectiveMaxPages || pageDelayMs <= 0) {
            return true;
        }

        try {
            Thread.sleep(pageDelayMs);
            return true;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.warn("[NewsAPI] Pagination delay interrupted; stopping further fetch work.");
            return false;
        }
    }

    private void logPageSummary(int page, int saved, int duplicates, int skipped) {
        log.info("[NewsAPI] Page {} — saved={} duplicates={} skipped={}",
                page, saved, duplicates, skipped);
    }

    private List<RawArticle> finish(
            List<RawArticle> savedArticles,
            int savedCount,
            int duplicateCount,
            int skippedCount
    ) {
        log.info("[NewsAPI] Completed — totalSaved={} duplicates={} skipped={}",
                savedCount, duplicateCount, skippedCount);
        return savedArticles;
    }
}

package com.news_aggregator.backend.service.fetchers;

import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.repository.RawArticleRepository;
import com.news_aggregator.backend.repository.SourceFetchLogRepository;
import com.news_aggregator.backend.service.filters.EsgFilterService;
import com.news_aggregator.backend.service.filters.TextNormalizerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class NewsApiFetcher extends AbstractFetcher implements RawNewsSourceFetcher {

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

    private static final String BASIC_QUERY =
            "(ESG OR sustainability OR sustainable OR climate OR renewable OR green OR environment OR carbon OR \"net zero\")";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public NewsApiFetcher(SourceFetchLogRepository logRepo,
                          RestTemplate restTemplate,
                          RawArticleRepository rawRepo,
                          EsgFilterService filter,
                          TextNormalizerService normalizer) {
        super(logRepo);
        this.restTemplate = restTemplate;
        this.rawRepo = rawRepo;
        this.filter = filter;
        this.normalizer = normalizer;
    }

    @Override
    public String getSourceName() {
        return "NewsAPI";
    }

    @Override
    public List<RawArticle> fetchArticles(int limit) {
        List<RawArticle> saved = new ArrayList<>();
        int savedCount = 0, duplicateCount = 0;
        int page = 1;
        int consecutiveDuplicatePages = 0;

        OffsetDateTime lastFetched = getLastFetched(getSourceName());
        String fromParam = lastFetched != null
                ? "&from=" + lastFetched.format(ISO)
                : "&from=" + OffsetDateTime.now().minusDays(1).format(ISO);

        System.out.println("🕒 Last fetched for NewsAPI: " + lastFetched);
        OffsetDateTime newestFetched = lastFetched;

        outer:
        while (page <= 10) {
            try {
                String url = String.format(
                        "%s/everything?q=%s&language=%s&sortBy=publishedAt&pageSize=50&page=%d%s&apiKey=%s",
                        baseUrl, BASIC_QUERY, language, page, fromParam, apiKey
                );

                ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                        url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
                );

                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) break;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> articles =
                        (List<Map<String, Object>>) resp.getBody().get("articles");
                if (articles == null || articles.isEmpty()) break;

                int pageDuplicates = 0;
                for (Map<String, Object> item : articles) {
                    try {
                        String title = normalizer.normalize((String) item.get("title"));
                        String description = normalizer.normalize((String) item.get("description"));
                        String content = normalizer.normalize((String) item.get("content"));
                        String urlToArticle = (String) item.get("url");

                        String sourceName = null;
                        Object src = item.get("source");
                        if (src instanceof Map<?, ?> mapSrc)
                            sourceName = (String) mapSrc.get("name");

                        // --- Duplicate filtering ---
                        if (urlToArticle != null && rawRepo.existsByUrl(urlToArticle)) {
                            pageDuplicates++;
                            continue;
                        }
                        if (title != null && sourceName != null &&
                                rawRepo.existsByTitleAndSourceName(title, sourceName)) {
                            pageDuplicates++;
                            continue;
                        }

                        if (!filter.isEsgRelevant(title, description, content)) continue;

                        String publishedAtStr = (String) item.get("publishedAt");
                        OffsetDateTime publishedAt = publishedAtStr != null
                                ? OffsetDateTime.parse(publishedAtStr)
                                : OffsetDateTime.now();

                        if (lastFetched != null && publishedAt.isBefore(lastFetched)) {
                            System.out.println("⏹ Reached older NewsAPI articles, stopping.");
                            break outer;
                        }

                        RawArticle raw = new RawArticle();
                        raw.setApiSource(getSourceName());
                        raw.setTitle(title);
                        raw.setDescription(description);
                        raw.setContent(content);
                        raw.setUrl(urlToArticle);
                        raw.setImageUrl((String) item.get("urlToImage"));
                        raw.setSourceName(sourceName);
                        raw.setPublishedAt(publishedAt);
                        raw.setRawJson(item);

                        rawRepo.save(raw);
                        System.out.printf("✅ [NewsAPI] Stored article #%d: \"%s\"%n", savedCount + 1, title);
                        saved.add(raw);
                        savedCount++;
                        if (newestFetched == null || publishedAt.isAfter(newestFetched))
                            newestFetched = publishedAt;

                        if (limit > 0 && savedCount >= limit) break outer;
                    } catch (Exception e) {
                        System.out.println("⚠️ NewsAPI article parse error: " + e.getMessage());
                    }
                }

                if (pageDuplicates == articles.size()) consecutiveDuplicatePages++;
                else consecutiveDuplicatePages = 0;
                if (consecutiveDuplicatePages >= 2) {
                    System.out.println("⏹ Detected duplicate pages, stopping fetch.");
                    break;
                }

                System.out.printf("📄 [NewsAPI] Page %d — Saved: %d | Duplicates: %d%n",
                        page, savedCount, duplicateCount);

                // ✅ Add randomized small delay (2–4 seconds)
                long delay = 2000 + (long) (Math.random() * 2000);
                System.out.printf("⏳ Waiting %.1f seconds before next page...%n", delay / 1000.0);
                Thread.sleep(delay);

                // ✅ Periodic cooldown every 3 pages
                if (page % 3 == 0) {
                    System.out.println("💤 Cooling down for 10 seconds to stay polite...");
                    Thread.sleep(10000);
                }

            } catch (HttpClientErrorException.TooManyRequests e) {
                // ✅ Handle API rate-limit (HTTP 429)
                System.out.println("⚠️ Rate limit hit — backing off for 60 seconds...");
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException ignored) {}
                continue; // retry same page
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    System.out.println("❌ Invalid NewsAPI key or quota exhausted.");
                    break;
                } else {
                    System.out.println("⚠️ HTTP error: " + e.getStatusCode());
                }
            } catch (Exception e) {
                System.out.println("⚠️ [NewsAPI] Error: " + e.getMessage());
            }

            page++;
        }

        updateLastFetched(getSourceName(), newestFetched, savedCount);
        System.out.printf("✅ [NewsAPI] Done — Saved: %d | Newest fetched: %s%n",
                savedCount, newestFetched);

        return saved;
    }
}

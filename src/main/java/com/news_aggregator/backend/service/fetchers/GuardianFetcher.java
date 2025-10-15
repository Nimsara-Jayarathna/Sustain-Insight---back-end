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
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;

@Component
public class GuardianFetcher extends AbstractFetcher implements RawNewsSourceFetcher {

    private final RestTemplate restTemplate;
    private final RawArticleRepository rawRepo;
    private final EsgFilterService filter;
    private final TextNormalizerService normalizer;

    @Value("${guardian.url}")
    private String baseUrl;

    @Value("${guardian.apiKey}")
    private String apiKey;

    public GuardianFetcher(SourceFetchLogRepository logRepo,
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
        return "The Guardian";
    }

    @Override
    public List<RawArticle> fetchArticles(int limit) {
        List<RawArticle> saved = new ArrayList<>();
        int savedCount = 0;
        int page = 1;

        OffsetDateTime lastFetched = getLastFetched(getSourceName());
        String fromDate = lastFetched != null
                ? "&from-date=" + lastFetched.toLocalDate()
                : "&from-date=" + OffsetDateTime.now().minusDays(1).toLocalDate();

        OffsetDateTime newestFetched = lastFetched;

        System.out.printf("🚀 Fetching from source: %s (since %s)%n",
                getSourceName(),
                lastFetched != null ? lastFetched.toLocalDate() : "yesterday");

        outer:
        while (page <= 10) {
            try {
                // --- Build request URL ---
                String url = String.format(
                        "%s/search?q=climate OR sustainability OR environment"
                                + "&show-fields=bodyText,headline,trailText,thumbnail,firstPublicationDate,byline"
                                + "&show-tags=contributor,keyword"
                                + "&order-by=newest&page=%d&page-size=50%s&api-key=%s",
                        baseUrl, page, fromDate, apiKey
                );

                ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                        url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
                );

                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                    System.out.printf("⚠️ [Guardian] Bad response (HTTP %d)%n", resp.getStatusCodeValue());
                    break;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) resp.getBody().get("response");
                if (data == null || !data.containsKey("results")) break;

                // ✅ Detect total pages
                int totalPages = ((Number) data.getOrDefault("pages", 1)).intValue();
                if (page > totalPages) {
                    System.out.printf("⏹ No more Guardian pages available (total %d). Stopping.%n", totalPages);
                    break;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> articles = (List<Map<String, Object>>) data.get("results");
                if (articles.isEmpty()) {
                    System.out.println("ℹ️ No articles found on this page.");
                    break;
                }

                int pageDuplicates = 0;
                for (Map<String, Object> item : articles) {
                    try {
                        String webUrl = (String) item.get("webUrl");
                        if (webUrl == null || webUrl.isBlank()) continue;

                        @SuppressWarnings("unchecked")
                        Map<String, Object> fields = (Map<String, Object>) item.get("fields");
                        if (fields == null) continue;

                        String title = normalizer.normalize((String) fields.get("headline"));
                        String description = normalizer.normalize((String) fields.get("trailText"));
                        String content = normalizer.normalize((String) fields.get("bodyText"));
                        String imageUrl = (String) fields.get("thumbnail");
                        String publishedAtStr = (String) fields.get("firstPublicationDate");

                        // --- Duplicate filtering ---
                        if (rawRepo.existsByUrl(webUrl)
                                || (title != null && rawRepo.existsByTitleAndSourceName(title, getSourceName()))) {
                            pageDuplicates++;
                            continue;
                        }

                        // --- ESG relevance check ---
                        if (!filter.isEsgRelevant(title, description, content)) continue;

                        OffsetDateTime publishedAt = publishedAtStr != null
                                ? OffsetDateTime.parse(publishedAtStr)
                                : OffsetDateTime.now();

                        // Stop if older than lastFetched
                        if (lastFetched != null && publishedAt.isBefore(lastFetched)) {
                            System.out.println("⏹ Reached older Guardian articles, stopping.");
                            break outer;
                        }

                        // --- Build and save article ---
                        RawArticle raw = new RawArticle();
                        raw.setApiSource(getSourceName());
                        raw.setTitle(title);
                        raw.setDescription(description);
                        raw.setContent(content);
                        raw.setUrl(webUrl);
                        raw.setImageUrl(imageUrl);
                        raw.setSourceName(getSourceName());
                        raw.setPublishedAt(publishedAt);
                        raw.setRawJson(item);

                        rawRepo.save(raw);
                        saved.add(raw);
                        savedCount++;

                        System.out.printf("✅ [Guardian] #%d: %s%n", savedCount, title);

                        if (newestFetched == null || publishedAt.isAfter(newestFetched))
                            newestFetched = publishedAt;

                        if (limit > 0 && savedCount >= limit) break outer;

                    } catch (Exception e) {
                        System.out.println("⚠️ Guardian article parse error: " + e.getMessage());
                    }
                }

                // --- Stop if mostly duplicates ---
                if (pageDuplicates == articles.size()) {
                    System.out.println("⏹ All articles on this page already exist. Stopping early.");
                    break;
                }

                System.out.printf("📄 [Guardian] Page %d processed — Total saved so far: %d%n",
                        page, savedCount);

                // --- Human-like delay ---
                long delay = 2000 + (long) (Math.random() * 2000);
                System.out.printf("⏳ Waiting %.1f seconds before next page...%n", delay / 1000.0);
                Thread.sleep(delay);

                // --- Periodic cooldown ---
                if (page % 3 == 0) {
                    System.out.println("💤 Cooling down for 10 seconds to stay polite...");
                    Thread.sleep(10000);
                }

            } catch (HttpClientErrorException.TooManyRequests e) {
                System.out.println("⚠️ Guardian rate limit hit — pausing for 60 seconds...");
                try { Thread.sleep(60000); } catch (InterruptedException ignored) {}
                continue;
            } catch (Exception e) {
                System.out.printf("⚠️ [Guardian] Error on page %d: %s%n", page, e.getMessage());
                break;
            }
            page++;
        }

        if (savedCount == 0) {
            System.out.println("ℹ️ No new Guardian articles found in this run.");
        }

        updateLastFetched(getSourceName(), newestFetched, savedCount);
        System.out.printf("✅ [Guardian] Done — Total Saved: %d | Newest fetched: %s%n",
                savedCount, newestFetched);

        return saved;
    }
}

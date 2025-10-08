package com.news_aggregator.backend.service.fetchers;

import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.repository.RawArticleRepository;
import com.news_aggregator.backend.service.filters.EsgFilterService;
import com.news_aggregator.backend.service.filters.TextNormalizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GuardianFetcher implements RawNewsSourceFetcher {

    private final RestTemplate restTemplate;
    private final RawArticleRepository rawRepo;
    private final EsgFilterService esgFilterService;
    private final TextNormalizerService textNormalizer;

    @Value("${guardian.api.url}")
    private String baseUrl;

    @Value("${guardian.api.key}")
    private String apiKey;

    private static final String SECTION = "environment";
    private static final int PAGE_SIZE = 50;

    @Override
    public String getSourceName() {
        return "The Guardian";
    }

    @Override
    public List<RawArticle> fetchArticles(int limit) {
        List<RawArticle> savedArticles = new ArrayList<>();
        int savedCount = 0, duplicateCount = 0, skippedCount = 0;
        int page = 1;

        outer:
        while (page <= 10) {
            try {
                String url = String.format(
                        "%s/search?section=%s&page-size=%d&page=%d&show-fields=all&api-key=%s",
                        baseUrl, SECTION, PAGE_SIZE, page, apiKey
                );

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
                );

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) break;

                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) body.get("response");
                if (responseMap == null) break;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
                if (results == null || results.isEmpty()) break;

                for (Map<String, Object> item : results) {
                    try {
                        String title = textNormalizer.cleanHtml((String) item.get("webTitle"));
                        String articleUrl = (String) item.get("webUrl");
                        String publishedAt = (String) item.get("webPublicationDate");

                        @SuppressWarnings("unchecked")
                        Map<String, Object> fields = (Map<String, Object>) item.get("fields");
                        String description = textNormalizer.cleanHtml((String) (fields != null ? fields.get("trailText") : null));
                        String content = textNormalizer.cleanHtml((String) (fields != null ? fields.get("body") : null));
                        String thumbnail = fields != null ? (String) fields.get("thumbnail") : null;

                        // Deduplication
                        if (articleUrl != null && rawRepo.existsByUrl(articleUrl)) {
                            duplicateCount++;
                            continue;
                        }

                        if (title != null && rawRepo.existsByTitleAndSourceName(title, getSourceName())) {
                            duplicateCount++;
                            continue;
                        }

                        // ESG filter
                        if (!esgFilterService.isEsgRelevant(title, description, content)) {
                            skippedCount++;
                            continue;
                        }

                        // Save
                        RawArticle raw = new RawArticle();
                        raw.setApiSource(getSourceName());
                        raw.setTitle(title);
                        raw.setDescription(description);
                        raw.setContent(content);
                        raw.setUrl(articleUrl);
                        raw.setImageUrl(thumbnail);
                        raw.setSourceName(getSourceName());

                        if (publishedAt != null)
                            raw.setPublishedAt(OffsetDateTime.parse(publishedAt));

                        raw.setRawJson(item);
                        rawRepo.save(raw);
                        savedArticles.add(raw);
                        savedCount++;

                        if (limit > 0 && savedCount >= limit) break outer;

                    } catch (Exception e) {
                        System.out.println("⚠️ Failed to save Guardian article: " + e.getMessage());
                    }
                }

                System.out.printf("📄 Guardian Page %d — Saved: %d | Duplicates: %d | Skipped: %d%n",
                        page, savedCount, duplicateCount, skippedCount);

                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("⚠️ Error on Guardian page " + page + ": " + e.getMessage());
            }
            page++;
        }

        System.out.printf("✅ Guardian fetch finished — Total Saved: %d | Duplicates: %d | Skipped: %d%n",
                savedCount, duplicateCount, skippedCount);

        return savedArticles;
    }
}

package com.news_aggregator.backend.service.fetchers;

import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.service.filters.EsgFilterService;
import com.news_aggregator.backend.repository.RawArticleRepository;
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
    private final EsgFilterService filter;

    @Value("${guardian.url}")
    private String baseUrl;

    @Value("${guardian.apiKey}")
    private String apiKey;

    @Override
    public String getSourceName() {
        return "The Guardian";
    }

    @Override
    public List<RawArticle> fetchArticles(int limit) {
        List<RawArticle> savedArticles = new ArrayList<>();
        int savedCount = 0, duplicateCount = 0;
        int page = 1;

        outer:
        while (page <= 10) {
            try {
                String url = String.format(
                        "%s/search?q=climate OR sustainability OR environment&show-fields=bodyText,headline,trailText,thumbnail,firstPublicationDate,byline&api-key=%s&page=%d&page-size=50",
                        baseUrl, apiKey, page
                );

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
                );

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) break;

                Map<String, Object> resp = response.getBody();
                Map<String, Object> responseData = (Map<String, Object>) resp.get("response");
                if (responseData == null || !responseData.containsKey("results")) break;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> articles =
                        (List<Map<String, Object>>) responseData.get("results");

                for (Map<String, Object> item : articles) {
                    try {
                        String webUrl = (String) item.get("webUrl");
                        Map<String, Object> fields = (Map<String, Object>) item.get("fields");

                        String title = fields != null ? (String) fields.get("headline") : null;
                        String description = fields != null ? (String) fields.get("trailText") : null;
                        String content = fields != null ? (String) fields.get("bodyText") : null;
                        String imageUrl = fields != null ? (String) fields.get("thumbnail") : null;
                        String publishedAt = fields != null ? (String) fields.get("firstPublicationDate") : null;

                        if (webUrl != null && rawRepo.existsByUrl(webUrl)) {
                            duplicateCount++;
                            continue;
                        }
                        if (title != null && rawRepo.existsByTitleAndSourceName(title, getSourceName())) {
                            duplicateCount++;
                            continue;
                        }

                        if (!filter.isEsgRelevant(title, description, content)) continue;


                        RawArticle raw = new RawArticle();
                        raw.setApiSource(getSourceName());
                        raw.setTitle(title);
                        raw.setDescription(description);
                        raw.setContent(content);
                        raw.setUrl(webUrl);
                        raw.setImageUrl(imageUrl);
                        raw.setSourceName(getSourceName());
                        if (publishedAt != null)
                            raw.setPublishedAt(OffsetDateTime.parse(publishedAt));
                        raw.setRawJson(item);
                        rawRepo.save(raw);
                        savedArticles.add(raw);
                        savedCount++;

                        if (limit > 0 && savedCount >= limit) break outer;
                    } catch (Exception e) {
                        System.out.println("⚠️ Guardian save failed: " + e.getMessage());
                    }
                }

                System.out.printf("📄 [Guardian] Page %d — Saved: %d | Duplicates: %d%n",
                        page, savedCount, duplicateCount);

                Thread.sleep(2000);

            } catch (Exception e) {
                System.out.println("⚠️ [Guardian] Error on page " + page + ": " + e.getMessage());
            }
            page++;
        }

        System.out.printf("✅ [Guardian] Completed — Total Saved: %d | Duplicates: %d%n",
                savedCount, duplicateCount);
        return savedArticles;
    }
}

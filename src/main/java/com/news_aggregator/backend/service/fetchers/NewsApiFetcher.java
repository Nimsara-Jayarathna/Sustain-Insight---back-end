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
public class NewsApiFetcher implements RawNewsSourceFetcher {

    private final RestTemplate restTemplate;
    private final RawArticleRepository rawRepo;
    private final EsgFilterService filter;

    @Value("${newsapi.url}")
    private String baseUrl;

    @Value("${newsapi.apiKey}")
    private String apiKey;

    @Value("${newsapi.language:en}")
    private String language;

    private static final String BASIC_QUERY =
            "(ESG OR sustainability OR sustainable OR climate OR renewable OR green OR environment OR carbon OR \"net zero\")";

    @Override
    public String getSourceName() {
        return "NewsAPI";
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
                        "%s/everything?q=%s&language=%s&pageSize=50&page=%d&apiKey=%s",
                        baseUrl, BASIC_QUERY, language, page, apiKey
                );

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
                );

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) break;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> articles =
                        (List<Map<String, Object>>) response.getBody().get("articles");

                if (articles == null || articles.isEmpty()) break;

                for (Map<String, Object> item : articles) {
                    try {
                        String title = (String) item.get("title");
                        String description = (String) item.get("description");
                        String content = (String) item.get("content");
                        if (content != null)
                            content = content.replaceAll("\\[\\+\\d+ chars\\]", "").trim();

                        String articleUrl = (String) item.get("url");
                        String sourceName = null;
                        Object src = item.get("source");
                        if (src instanceof Map<?, ?> srcMap)
                            sourceName = (String) srcMap.get("name");

                        // 🔹 Deduplication (DB-based)
                        if (articleUrl != null && rawRepo.existsByUrl(articleUrl)) {
                            duplicateCount++;
                            continue;
                        }
                        if (title != null && sourceName != null &&
                                rawRepo.existsByTitleAndSourceName(title, sourceName)) {
                            duplicateCount++;
                            continue;
                        }

                        if (!filter.isEsgRelevant(title, description, content)) continue;

                        RawArticle raw = new RawArticle();
                        raw.setApiSource(getSourceName());
                        raw.setTitle(title);
                        raw.setDescription(description);
                        raw.setContent(content);
                        raw.setUrl(articleUrl);
                        raw.setImageUrl((String) item.get("urlToImage"));
                        raw.setSourceName(sourceName);

                        String publishedAt = (String) item.get("publishedAt");
                        if (publishedAt != null)
                            raw.setPublishedAt(OffsetDateTime.parse(publishedAt));

                        raw.setRawJson(item);
                        rawRepo.save(raw);
                        savedArticles.add(raw);
                        savedCount++;

                        if (limit > 0 && savedCount >= limit) break outer;

                    } catch (Exception e) {
                        System.out.println("⚠️ NewsAPI save failed: " + e.getMessage());
                    }
                }

                System.out.printf("📄 [NewsAPI] Page %d — Saved: %d | Duplicates: %d | Skipped: %d%n",
                        page, savedCount, duplicateCount, skippedCount);

                Thread.sleep(2000);

            } catch (Exception e) {
                System.out.println("⚠️ [NewsAPI] Error on page " + page + ": " + e.getMessage());
            }
            page++;
        }

        System.out.printf("✅ [NewsAPI] Completed — Total Saved: %d | Duplicates: %d | Skipped: %d%n",
                savedCount, duplicateCount, skippedCount);
        return savedArticles;
    }
}

package com.news_aggregator.backend.service.filters;

import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.repository.RawArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TfidfExportService {

    private final RawArticleRepository rawRepo;

    /**
     * 🔹 Build a full export JSON structure containing all raw article details
     * and their similarity relations.
     */
    public Map<String, Object> buildFullJsonExport(List<Map<String, Object>> tfidfPairs, double threshold) {
        List<RawArticle> allArticles = rawRepo.findAll();

        // 🧭 Build lookup for quick ID access
        Map<Long, RawArticle> articleMap = allArticles.stream()
                .collect(Collectors.toMap(RawArticle::getId, a -> a));

        // 🧩 Build bidirectional relationship map
        Map<Long, List<Map<String, Object>>> relations = new HashMap<>();
        for (Map<String, Object> pair : tfidfPairs) {
            double sim = Double.parseDouble(pair.get("similarity").toString());
            if (sim < threshold) continue;

            Long id1 = Long.parseLong(pair.get("id1").toString());
            Long id2 = Long.parseLong(pair.get("id2").toString());

            relations.computeIfAbsent(id1, k -> new ArrayList<>())
                    .add(Map.of("related_id", id2, "similarity", sim));

            relations.computeIfAbsent(id2, k -> new ArrayList<>())
                    .add(Map.of("related_id", id1, "similarity", sim));
        }

        // 🏗️ Build article objects
        List<Map<String, Object>> articles = new ArrayList<>();
        for (RawArticle a : allArticles) {
            Map<String, Object> articleJson = new LinkedHashMap<>();
            articleJson.put("id", a.getId());
            articleJson.put("api_source", a.getApiSource());
            articleJson.put("source_name", a.getSourceName());
            articleJson.put("title", a.getTitle());
            articleJson.put("description", a.getDescription());
            articleJson.put("content", a.getContent());
            articleJson.put("url", a.getUrl());
            articleJson.put("image_url", a.getImageUrl());
            articleJson.put("published_at", a.getPublishedAt());
            articleJson.put("fetched_at", a.getFetchedAt());
            articleJson.put("relations", relations.getOrDefault(a.getId(), Collections.emptyList()));

            articles.add(articleJson);
        }

        // 🪶 Meta information
        Map<String, Object> meta = Map.of(
                "source", "raw_articles",
                "total_articles", allArticles.size(),
                "similarity_threshold", threshold,
                "generated_at", OffsetDateTime.now().toString()
        );

        return Map.of("meta", meta, "articles", articles);
    }
}

package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.repository.RawArticleRepository;
import com.news_aggregator.backend.service.filters.TfidfSimilarityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 🧩 Admin utilities for data processing:
 *  - TF-IDF cosine similarity analysis
 *  - (Later) export full structured JSON with relationships
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TfidfSimilarityService tfidfService;
    private final RawArticleRepository rawArticleRepository;

    /**
     * 🔹 Compare all raw articles and return pairs above threshold.
     * Example: GET http://localhost:8080/admin/check-similarity?threshold=0.5
     */
    @GetMapping("/check-similarity")
    public ResponseEntity<List<Map<String, Object>>> checkSimilarity(
            @RequestParam(defaultValue = "0.5") double threshold) {

        // Step 1️⃣ — Load articles from DB
        var articles = rawArticleRepository.findAll();

        // Step 2️⃣ — Map to lightweight TF-IDF input
        var articleList = articles.stream()
                .map(a -> new TfidfSimilarityService.ArticleMinimal(
                        a.getId(),
                        a.getTitle(),
                        a.getDescription(),
                        a.getContent()
                ))
                .toList();

        // Step 3️⃣ — Run TF-IDF cosine similarity
        var pairs = tfidfService.findSimilarArticles(articleList, threshold);

        // Step 4️⃣ — Format minimal result JSON
        var results = new ArrayList<Map<String, Object>>();
        for (var p : pairs) {
            results.add(Map.of(
                    "id1", p.id1(),
                    "id2", p.id2(),
                    "similarity", String.format("%.3f", p.similarity())
            ));
        }

        System.out.printf("✅ TF-IDF check complete — %d pairs found ≥ %.2f%n", results.size(), threshold);
        return ResponseEntity.ok(results);
    }
}

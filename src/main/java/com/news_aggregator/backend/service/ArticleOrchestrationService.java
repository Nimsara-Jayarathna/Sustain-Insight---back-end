package com.news_aggregator.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.repository.RawArticleRepository;
import com.news_aggregator.backend.service.ai.ArticlePromptBuilderService;
import com.news_aggregator.backend.service.ai.ArticleSynthesisService;
import com.news_aggregator.backend.service.filters.ClusteredTfidfExportService;
import com.news_aggregator.backend.service.filters.TfidfSimilarityService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArticleOrchestrationService {

    private final RawArticleRepository rawArticleRepository;
    private final TfidfSimilarityService tfidfSimilarityService;
    private final ClusteredTfidfExportService clusteredTfidfExportService;
    private final ArticlePromptBuilderService promptBuilderService;
    private final ArticleSynthesisService synthesisService;
    private final CategoryService categoryService;
    private final SourceService sourceService;
    private final ArticleService articleService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void runOnceAtStartup() {
        System.out.println("🚀 Running orchestration once at startup...");
        orchestrateArticleProcessing();
        System.out.println("✅ Orchestration finished.");
    }

    // @Scheduled(cron = "0 0 * * * *") // Runs every hour
    public void scheduledOrchestration() {
        System.out.println("🚀 Starting scheduled article orchestration...");
        orchestrateArticleProcessing();
        System.out.println("✅ Scheduled article orchestration finished.");
    }

    public void orchestrateArticleProcessing() {
        try {
            // Step 1: Fetch raw articles
            List<RawArticle> rawArticles = rawArticleRepository.findAll();
            List<TfidfSimilarityService.ArticleMinimal> articleList = rawArticles.stream()
                    .map(a -> new TfidfSimilarityService.ArticleMinimal(
                            a.getId(),
                            a.getTitle(),
                            a.getDescription(),
                            a.getContent()
                    ))
                    .toList();

            // Step 2: Generate similarity scores
            List<TfidfSimilarityService.SimilarityResult> similarityPairs = tfidfSimilarityService.findSimilarArticles(articleList, 0.5);
            List<Map<String, Object>> tfidfPairs = new ArrayList<>();
            for (TfidfSimilarityService.SimilarityResult p : similarityPairs) {
                tfidfPairs.add(Map.of(
                        "id1", p.id1(),
                        "id2", p.id2(),
                        "similarity", String.format("%.3f", p.similarity())
                ));
            }

            // Step 3: Build article clusters
            Map<String, Object> clusteredData = clusteredTfidfExportService.buildClusteredExport(tfidfPairs, 0.5);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> clusters = (List<Map<String, Object>>) clusteredData.get("clusters");

            // Step 4: Synthesize articles with AI
            List<Map<String, Object>> availableCategories = categoryService.getAllAsMap();
            List<Map<String, Object>> availableSources = sourceService.getAllAsMap();
            String engineeredPromptJson = promptBuilderService.buildEngineeredPrompt(clusters, availableCategories, availableSources);
            String geminiResponse = synthesisService.generateUnifiedArticle(engineeredPromptJson);

            // Step 5: Save the synthesized articles
            List<Map<String, Object>> synthesizedArticles = objectMapper.readValue(geminiResponse, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            articleService.saveSynthesizedArticles(synthesizedArticles);

        } catch (Exception e) {
            System.err.println("❌ An error occurred during article orchestration:");
            e.printStackTrace();
        }
    }
}
package com.news_aggregator.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.repository.RawArticleRepository;
import com.news_aggregator.backend.service.ai.ArticlePromptBuilderService;
import com.news_aggregator.backend.service.ai.ArticleSynthesisService;
import com.news_aggregator.backend.service.filters.ClusteredTfidfExportService;
import com.news_aggregator.backend.service.filters.TfidfSimilarityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final SynthesisState synthesisState;

    @Value("${clustering.tfidf.threshold:0.5}")
    private double tfidfThreshold;

    public void orchestrateArticleProcessing() {
        if (synthesisState.isSynthesisInProgress()) {
            System.out.println("Synthesis is already in progress. Skipping orchestration.");
            return;
        }

        try {
            synthesisState.setSynthesisInProgress(true);
            System.out.println("🚀 Starting article orchestration...");

            // Step 1: Fetch raw articles that have not been processed
            List<RawArticle> rawArticles = rawArticleRepository.findByProcessedFalse();
            List<TfidfSimilarityService.ArticleMinimal> articleList = rawArticles.stream()
                    .map(a -> new TfidfSimilarityService.ArticleMinimal(
                            a.getId(),
                            a.getTitle(),
                            a.getDescription(),
                            a.getContent()
                    ))
                    .toList();

            // Step 2: Generate similarity scores
            List<TfidfSimilarityService.SimilarityResult> similarityPairs = tfidfSimilarityService.findSimilarArticles(articleList, tfidfThreshold);
            List<Map<String, Object>> tfidfPairs = new ArrayList<>();
            for (TfidfSimilarityService.SimilarityResult p : similarityPairs) {
                tfidfPairs.add(Map.of(
                        "id1", p.id1(),
                        "id2", p.id2(),
                        "similarity", String.format("%.3f", p.similarity())
                ));
            }

            // Step 3: Build article clusters
            Map<String, Object> clusteredData = clusteredTfidfExportService.buildClusteredExport(tfidfPairs, tfidfThreshold);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> clusters = (List<Map<String, Object>>) clusteredData.get("clusters");

            if (clusters.isEmpty()) {
                System.out.println("No new clusters to process.");
                return;
            }

            // Step 4: Synthesize articles with AI
            List<Map<String, Object>> availableCategories = categoryService.getAllAsMap();
            List<Map<String, Object>> availableSources = sourceService.getAllAsMap();
            String engineeredPromptJson = promptBuilderService.buildEngineeredPrompt(clusters, availableCategories, availableSources);
            String geminiResponse = synthesisService.generateUnifiedArticle(engineeredPromptJson);

            // Step 5: Save the synthesized articles
            List<Map<String, Object>> synthesizedArticles = objectMapper.readValue(geminiResponse, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            articleService.saveSynthesizedArticles(synthesizedArticles);

            // Step 6: Mark all articles that were part of the TF-IDF check as processed
            for (RawArticle rawArticle : rawArticles) {
                rawArticle.setProcessed(true);
            }
            rawArticleRepository.saveAll(rawArticles);

            System.out.println("✅ Article orchestration finished successfully.");

        } catch (Exception e) {
            System.err.println("❌ An error occurred during article orchestration:");
            e.printStackTrace();
        } finally {
            synthesisState.setSynthesisInProgress(false);
        }
    }
}
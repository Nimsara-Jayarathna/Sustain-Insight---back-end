package com.news_aggregator.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.news_aggregator.backend.service.ai.ArticlePromptBuilderService;
import com.news_aggregator.backend.service.ai.ArticleSynthesisService;
import com.news_aggregator.backend.service.CategoryService;
import com.news_aggregator.backend.service.SourceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai/synthesize")
@RequiredArgsConstructor
public class ArticleSynthesisController {

    private static final Logger log = LoggerFactory.getLogger(ArticleSynthesisController.class);

    private final ArticlePromptBuilderService promptBuilderService;
    private final ArticleSynthesisService synthesisService;
    private final CategoryService categoryService;
    private final SourceService sourceService;

    @PostMapping
    public ResponseEntity<?> synthesize(@RequestBody Map<String, Object> payload) {
        try {
            log.info("🔹 [1] Received synthesis request payload");
            log.debug("Payload: {}", payload);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> clusters = 
                    (List<Map<String, Object>>) payload.getOrDefault("clusters", List.of());

            List<Map<String, Object>> availableCategories = categoryService.getAllAsMap();
            List<Map<String, Object>> availableSources = sourceService.getAllAsMap();

            log.info("🔹 [2] Loaded {} categories and {} sources", availableCategories.size(), availableSources.size());

            String engineeredPromptJson = promptBuilderService.buildEngineeredPrompt(
                    clusters, availableCategories, availableSources);

            log.info("🔹 [3] Engineered Prompt JSON built successfully ({} chars)", engineeredPromptJson.length());

            String geminiResponse = synthesisService.generateUnifiedArticle(engineeredPromptJson);

            log.info("🔹 [4] Gemini synthesis complete");

            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> synthesizedArticles = objectMapper.readValue(geminiResponse, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "categories", availableCategories.size(),
                    "sources", availableSources.size(),
                    "prompt_length", engineeredPromptJson.length(),
                    "synthesized_result", synthesizedArticles
            ));

        } catch (Exception e) {
            log.error("❌ Error during article synthesis", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}
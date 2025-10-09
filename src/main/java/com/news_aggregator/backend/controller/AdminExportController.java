package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.service.filters.ClusteredTfidfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin export controller for AI preprocessing.
 * Generates clustered article JSON structures for AI deduplication/synthesis.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminExportController {

    private final ClusteredTfidfExportService exportService;

    /**
     * 🔹 Generate clustered article+relation JSON for AI processing.
     * Example:
     * POST http://localhost:8080/admin/tfidf-export/clustered?threshold=0.5
     * Body = TF-IDF pair list [{id1,id2,similarity}, ...]
     */
    @PostMapping("/tfidf-export/clustered")
    public ResponseEntity<Map<String, Object>> generateExport(
            @RequestBody List<Map<String, Object>> tfidfPairs,
            @RequestParam(defaultValue = "0.5") double threshold) {

        Map<String, Object> exportJson = exportService.buildClusteredExport(tfidfPairs, threshold);
        return ResponseEntity.ok(exportJson);
    }
}

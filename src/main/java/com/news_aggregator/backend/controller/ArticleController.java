package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping(value = "/latest", produces = "application/json")
    public ResponseEntity<List<ArticleDto>> getLatestArticles(
            @RequestParam(defaultValue = "3") int limit) {
        if (limit < 1 || limit > 50) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(articleService.getLatestArticles(limit));
    }
}

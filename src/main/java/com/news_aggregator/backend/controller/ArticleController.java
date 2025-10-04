package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.dto.PagedResponse;
import com.news_aggregator.backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    // 🔹 Pagination settings
    @Value("${pagination.defaultSize:10}")   // default page size
    private int defaultPageSize;

    @Value("${pagination.maxSize:50}")       // max allowed page size
    private int maxPageSize;

    // 🔹 "latest articles" limit
    @Value("${articles.latest.defaultLimit:9}")  // fallback = 9
    private int latestDefaultLimit;

    @Value("${articles.latest.maxLimit:50}")     // fallback = 50
    private int latestMaxLimit;

    @GetMapping(value = "/latest", produces = "application/json")
    public ResponseEntity<List<ArticleDto>> getLatestArticles(
            @RequestParam(required = false) Integer limit) {

        int effectiveLimit = (limit == null || limit <= 0)
                ? latestDefaultLimit
                : Math.min(limit, latestMaxLimit);

        return ResponseEntity.ok(articleService.getLatestArticles(effectiveLimit));
    }

    @GetMapping(value = "/all", produces = "application/json")
    public ResponseEntity<PagedResponse<ArticleDto>> getAllArticles(
        @RequestParam(required = false, name = "category") List<Long> categoryIds,
        @RequestParam(required = false, name = "source") List<Long> sourceIds,
        @RequestParam(required = false, name = "search") String keyword,
        @RequestParam(required = false, name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(required = false) Integer size
    ) {
        int pageSize = (size == null || size <= 0)
            ? defaultPageSize
            : Math.min(size, maxPageSize);

        return ResponseEntity.ok(articleService.getAllArticles(
            categoryIds, sourceIds, keyword, date, page, pageSize
        ));
}

}

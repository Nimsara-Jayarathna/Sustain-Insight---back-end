package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.dto.PagedResponse;
import com.news_aggregator.backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/articles") // 🔒 authenticated routes
@RequiredArgsConstructor
public class AuthenticatedArticleController {

    private final ArticleService articleService;

    // 🔹 Pagination settings (shared with public controller)
    @Value("${pagination.defaultSize:10}")
    private int defaultPageSize;

    @Value("${pagination.maxSize:50}")
    private int maxPageSize;

    // --- Get All Articles (with bookmark state for user) ---
    @GetMapping("/all")
    public ResponseEntity<PagedResponse<ArticleDto>> getAllArticles(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, name = "category") java.util.List<Long> categoryIds,
            @RequestParam(required = false, name = "source") java.util.List<Long> sourceIds,
            @RequestParam(required = false, name = "search") String keyword,
            @RequestParam(required = false, name = "date") 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size
    ) {
        int pageSize = (size == null || size <= 0)
                ? defaultPageSize
                : Math.min(size, maxPageSize);

        return ResponseEntity.ok(articleService.getAllArticles(
                categoryIds, sourceIds, keyword, date, page, pageSize, userDetails
        ));
    }

    // --- Get Personalized Feed (with bookmark state for user) ---
    @GetMapping("/feed")
    public ResponseEntity<PagedResponse<ArticleDto>> getForYouFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size
    ) {
        int pageSize = (size == null || size <= 0)
                ? defaultPageSize
                : Math.min(size, maxPageSize);

        return ResponseEntity.ok(articleService.getForYouFeed(userDetails, page, pageSize));
    }
}

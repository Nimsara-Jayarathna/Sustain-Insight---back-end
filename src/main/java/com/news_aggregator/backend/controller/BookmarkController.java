package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.dto.PagedResponse;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // 🔹 Pagination settings (env-driven)
    @Value("${pagination.defaultSize:10}")
    private int defaultPageSize;

    @Value("${pagination.maxSize:50}")
    private int maxPageSize;

    /**
     * Add a bookmark for the given article
     */
    @PostMapping("/{articleId}")
    public ResponseEntity<Map<String, String>> addBookmark(
            @AuthenticationPrincipal User user,
            @PathVariable Long articleId) {
        bookmarkService.addBookmark(user, articleId);
        return ResponseEntity.ok(Map.of("status", "bookmarked", "articleId", articleId.toString()));
    }

    /**
     * Remove a bookmark for the given article
     */
    @DeleteMapping("/{articleId}")
    public ResponseEntity<Map<String, String>> removeBookmark(
            @AuthenticationPrincipal User user,
            @PathVariable Long articleId) {
        bookmarkService.removeBookmark(user, articleId);
        return ResponseEntity.ok(Map.of("status", "removed", "articleId", articleId.toString()));
    }

    /**
     * Get all bookmarks for the logged-in user (with pagination)
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ArticleDto>> getBookmarks(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") int page,   // ✅ 1-based like ArticleController
            @RequestParam(required = false) Integer size
    ) {
        int pageSize = (size == null || size <= 0)
                ? defaultPageSize
                : Math.min(size, maxPageSize);

        return ResponseEntity.ok(bookmarkService.getBookmarks(user, page, pageSize));
    }
}

package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.dto.PagedResponse;
import com.news_aggregator.backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class FeedController {

    private final ArticleService articleService;

    @Value("${pagination.defaultSize:10}")   // 👈 from env, fallback = 10
    private int defaultPageSize;

    @Value("${pagination.maxSize:50}")       // 👈 from env, fallback = 50
    private int maxPageSize;

    // @GetMapping("/feed")
    // public ResponseEntity<PagedResponse<ArticleDto>> getForYouFeed(
    //         @AuthenticationPrincipal UserDetails userDetails,
    //         @RequestParam(defaultValue = "1") int page,
    //         @RequestParam(required = false) Integer size
    // ) {
    //     int pageSize = (size == null || size <= 0) ? defaultPageSize : Math.min(size, maxPageSize);
    //     return ResponseEntity.ok(articleService.getForYouFeed(userDetails, page, pageSize));
    // }
}

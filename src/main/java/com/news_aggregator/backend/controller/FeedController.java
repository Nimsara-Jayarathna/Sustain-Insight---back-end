package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class FeedController {

    private final ArticleService articleService;

    @GetMapping("/feed")
    public ResponseEntity<List<ArticleDto>> getForYouFeed(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(articleService.getForYouFeed(userDetails));
    }
}

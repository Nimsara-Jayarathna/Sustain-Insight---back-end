package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.service.ArticleService;
import com.news_aggregator.backend.service.InsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;
    private final ArticleService articleService;

    @PostMapping("/{articleId}")
    public ResponseEntity<ArticleDto> addInsight(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long articleId
    ) {
        User user = (User) userDetails;
        Article article = articleService.getArticleEntity(articleId);

        insightService.addInsight(user, article);

        // return latest DTO (with updated count + state)
        return ResponseEntity.ok(articleService.mapToDto(article, user));
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<ArticleDto> removeInsight(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long articleId
    ) {
        User user = (User) userDetails;
        Article article = articleService.getArticleEntity(articleId);

        insightService.removeInsight(user, article);

        // return latest DTO (with updated count + state)
        return ResponseEntity.ok(articleService.mapToDto(article, user));
    }

    // Get total count for an article
    @GetMapping("/{articleId}/count")
    public ResponseEntity<Long> getInsightCount(@PathVariable Long articleId) {
        Article article = articleService.getArticleEntity(articleId);
        return ResponseEntity.ok(insightService.getCount(article));
    }

    // Check if current user has insighted this article
    @GetMapping("/{articleId}/me")
    public ResponseEntity<Boolean> isInsighted(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long articleId
    ) {
        User user = (User) userDetails;
        Article article = articleService.getArticleEntity(articleId);
        return ResponseEntity.ok(insightService.isInsighted(user, article));
    }
}

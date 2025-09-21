package com.news_aggregator.backend.service;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    @Autowired
    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<ArticleDto> getLatestArticles(int limit) {
        List<Article> articles = articleRepository.findTopNArticles(limit);
        return articles.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ArticleDto> getAllArticles() {
        List<Article> articles = articleRepository.findAllOrderedByPublishedAt();
        return articles.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ArticleDto mapToDto(Article article) {
        return new ArticleDto(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getImageUrl(),
                article.getPublishedAt(),
                article.getSources().stream().map(s -> s.getName()).toList(),
                article.getCategories().stream().map(c -> c.getName()).toList()
        );
    }
}

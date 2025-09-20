package com.news_aggregator.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ArticleDto {
    private Long id;
    private String title;
    private String summary;
    private String imageUrl;
    private LocalDateTime publishedAt;
    private List<String> sources;
    private List<String> categories;

    // --- Constructors ---
    public ArticleDto() {}

    public ArticleDto(Long id, String title, String summary, String imageUrl,
                      LocalDateTime publishedAt, List<String> sources, List<String> categories) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
        this.sources = sources;
        this.categories = categories;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
}

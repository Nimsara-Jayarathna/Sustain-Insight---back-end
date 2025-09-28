package com.news_aggregator.backend.config;

import com.news_aggregator.backend.service.NewsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupNewsFetcher implements CommandLineRunner {

    private final NewsService newsService;

    public StartupNewsFetcher(NewsService newsService) {
        this.newsService = newsService;
    }

    @Override
    public void run(String... args) {
        try {
            // Fetch 5 sustainability articles at startup
            newsService.fetchAndSaveArticles(5);
            System.out.println("✅ Startup fetch completed (5 articles).");
        } catch (Exception e) {
            System.err.println("⚠️ Startup fetch failed: " + e.getMessage());
        }
    }
}

package com.news_aggregator.backend.config;

import com.news_aggregator.backend.service.NewsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupNewsFetcher implements CommandLineRunner {

    private final NewsService newsService;
    private final int fetchingEnabled; // 0 or 1

    public StartupNewsFetcher(NewsService newsService,
                              @Value("${fetching.enabled}") int fetchingEnabled) {
        this.newsService = newsService;
        this.fetchingEnabled = fetchingEnabled;
    }

    @Override
    public void run(String... args) {
        if (fetchingEnabled == 0) {
            System.out.println("⏸ Startup fetch disabled (fetching.enabled=0).");
            return;
        }
        try {
            newsService.fetchAndSaveArticles(5);
            System.out.println("✅ Startup fetch completed (5 articles).");
        } catch (Exception e) {
            System.err.println("⚠️ Startup fetch failed: " + e.getMessage());
        }
    }
}

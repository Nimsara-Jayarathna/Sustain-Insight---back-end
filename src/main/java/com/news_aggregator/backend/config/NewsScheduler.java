package com.news_aggregator.backend.config;

import com.news_aggregator.backend.service.NewsService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class NewsScheduler {

    private final NewsService newsService;

    public NewsScheduler(NewsService newsService) {
        this.newsService = newsService;
    }

    // Run once every 1 minute, only after last run finishes
    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 10 * 1000) 
    public void fetchDailyNews() {
        try {
            // Fetch exactly 2 sustainability articles each run
            newsService.fetchAndSaveArticles(2);
            System.out.println("✅ Scheduled fetch completed (2 articles).");
        } catch (Exception e) {
            System.err.println("⚠️ Scheduled fetch failed: " + e.getMessage());
        }
    }
}

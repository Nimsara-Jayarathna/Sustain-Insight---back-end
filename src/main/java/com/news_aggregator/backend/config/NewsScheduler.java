package com.news_aggregator.backend.config;

import com.news_aggregator.backend.service.NewsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class NewsScheduler {

    private final NewsService newsService;
    private final int fetchingEnabled; // 0 or 1

    public NewsScheduler(NewsService newsService,
                         @Value("${fetching.enabled}") int fetchingEnabled) {
        this.newsService = newsService;
        this.fetchingEnabled = fetchingEnabled;
    }
    // Run once every 1 minute, only after last run finishes
    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 10 * 1000)
    public void fetchDailyNews() {
        if (fetchingEnabled == 0) {
            System.out.println("⏸ Scheduled fetch disabled (fetching.enabled=0).");
            return;
        }
        try {
            newsService.fetchAndSaveArticles(2);
            System.out.println("✅ Scheduled fetch completed (2 articles).");
        } catch (Exception e) {
            System.err.println("⚠️ Scheduled fetch failed: " + e.getMessage());
        }
    }
}

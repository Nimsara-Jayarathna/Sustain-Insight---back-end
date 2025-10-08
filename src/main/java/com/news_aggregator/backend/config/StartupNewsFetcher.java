package com.news_aggregator.backend.config;

import com.news_aggregator.backend.service.RawNewsFetcherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupNewsFetcher implements CommandLineRunner {

    private final RawNewsFetcherService rawNewsFetcherService;
    private final int fetchingEnabled; // 0 or 1

    public StartupNewsFetcher(RawNewsFetcherService rawNewsFetcherService,
                              @Value("${fetching.enabled:1}") int fetchingEnabled) {
        this.rawNewsFetcherService = rawNewsFetcherService;
        this.fetchingEnabled = fetchingEnabled;
    }

    @Override
    public void run(String... args) {
        if (fetchingEnabled == 0) {
            System.out.println("⏸ Startup fetch skipped (fetching.enabled=0).");
            return;
        }

        try {
            System.out.println("🚀 Performing startup fetch from all sources...");
            rawNewsFetcherService.fetchFromAllSources(1); // fetch up to 5 per source
            System.out.println("✅ Startup fetch completed successfully.");
        } catch (Exception e) {
            System.err.println("⚠️ Startup fetch failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

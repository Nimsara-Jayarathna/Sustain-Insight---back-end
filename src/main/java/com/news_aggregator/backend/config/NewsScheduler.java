package com.news_aggregator.backend.config;

import com.news_aggregator.backend.service.RawNewsFetcherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@EnableScheduling
public class NewsScheduler {

    private final RawNewsFetcherService rawNewsFetcherService;
    private final int fetchingEnabled;

    public NewsScheduler(RawNewsFetcherService rawNewsFetcherService,
                         @Value("${fetching.enabled:1}") int fetchingEnabled) {
        this.rawNewsFetcherService = rawNewsFetcherService;
        this.fetchingEnabled = fetchingEnabled;
    }

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Runs every 30 seconds (after an initial 10-second delay).
     */
    @Scheduled(fixedDelay = 30 * 1000, initialDelay = 10 * 1000)
    public void scheduledFetch() {
        String time = LocalDateTime.now().format(TIME_FMT);

        if (fetchingEnabled == 0) {
            System.out.printf("[%s] ⏸ Scheduled fetch skipped (fetching.enabled=0).%n", time);
            return;
        }

        try {
            System.out.printf("[%s] 🚀 Scheduled fetch started...%n", time);
            rawNewsFetcherService.fetchFromAllSources(1); // fetch limited articles from each source
            System.out.printf("[%s] ✅ Scheduled fetch completed.%n",
                    LocalDateTime.now().format(TIME_FMT));
        } catch (Exception e) {
            System.err.printf("[%s] ⚠️ Scheduled fetch failed: %s%n",
                    LocalDateTime.now().format(TIME_FMT), e.getMessage());
            e.printStackTrace();
        }
    }
}

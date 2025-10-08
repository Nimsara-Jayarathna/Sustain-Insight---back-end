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

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private boolean startupCompleted = false;

    public NewsScheduler(RawNewsFetcherService rawNewsFetcherService,
                         @Value("${fetching.enabled:1}") int fetchingEnabled) {
        this.rawNewsFetcherService = rawNewsFetcherService;
        this.fetchingEnabled = fetchingEnabled;
    }

    /**
     * Runs every 30 seconds, but only after startup fetch has completed.
     */
    @Scheduled(fixedDelay = 30 * 1000, initialDelay = 20 * 1000) // starts 20s after startup
    public void scheduledFetch() {
        String time = LocalDateTime.now().format(TIME_FMT);

        if (fetchingEnabled == 0) {
            System.out.printf("[%s] ⏸ Scheduled fetch skipped (fetching.enabled=0).%n", time);
            return;
        }

        if (!startupCompleted) {
            System.out.printf("[%s] ⏳ Startup fetch still initializing, skipping this cycle.%n", time);
            startupCompleted = true; // ✅ mark once startup has likely finished
            return;
        }

        try {
            System.out.printf("[%s] 🚀 Scheduled fetch started...%n", time);
            rawNewsFetcherService.fetchFromAllSources(5);
            System.out.printf("[%s] ✅ Scheduled fetch completed.%n",
                    LocalDateTime.now().format(TIME_FMT));

        } catch (Exception e) {
            System.err.printf("[%s] ⚠️ Scheduled fetch failed: %s%n",
                    LocalDateTime.now().format(TIME_FMT), e.getMessage());
            e.printStackTrace();
        }
    }
}

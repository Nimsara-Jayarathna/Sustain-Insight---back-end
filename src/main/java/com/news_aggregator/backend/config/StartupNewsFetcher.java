package com.news_aggregator.backend.config;

import com.news_aggregator.backend.service.RawNewsFetcherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class StartupNewsFetcher implements CommandLineRunner {

    private final RawNewsFetcherService rawNewsFetcherService;
    private final int fetchingEnabled;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StartupNewsFetcher(RawNewsFetcherService rawNewsFetcherService,
                              @Value("${fetching.enabled:1}") int fetchingEnabled) {
        this.rawNewsFetcherService = rawNewsFetcherService;
        this.fetchingEnabled = fetchingEnabled;
    }

    @Override
    public void run(String... args) {
        String time = LocalDateTime.now().format(TIME_FMT);

        if (fetchingEnabled == 0) {
            System.out.printf("[%s] ⏸ Startup fetch skipped (fetching.enabled=0).%n", time);
            return;
        }

        try {
            System.out.printf("[%s] 🚀 Performing startup fetch from all sources...%n", time);
            rawNewsFetcherService.fetchFromAllSources(5); // fetch 1 article per source (or more if needed)
            System.out.printf("[%s] ✅ Startup fetch completed successfully.%n",
                    LocalDateTime.now().format(TIME_FMT));

        } catch (Exception e) {
            System.err.printf("[%s] ⚠️ Startup fetch failed: %s%n",
                    LocalDateTime.now().format(TIME_FMT), e.getMessage());
            e.printStackTrace();
        }

        // 🔹 Small intentional delay before scheduler starts
        try {
            System.out.println("⏳ Waiting 10 seconds before starting scheduled fetching...");
            Thread.sleep(10_000);
        } catch (InterruptedException ignored) {
        }
    }
}

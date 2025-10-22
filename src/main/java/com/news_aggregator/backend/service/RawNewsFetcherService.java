package com.news_aggregator.backend.service;

import com.news_aggregator.backend.service.fetchers.RawNewsSourceFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RawNewsFetcherService {

    private final List<RawNewsSourceFetcher> fetchers; // auto-injected

    /**
     * Fetches articles from all registered fetchers.
     * @param perSourceLimit number of articles to save from each source
     */
    public void fetchFromAllSources(int perSourceLimit) {
        int totalSaved = 0;
        for (RawNewsSourceFetcher fetcher : fetchers) {
            System.out.printf("🚀 Fetching from source: %s%n", fetcher.getSourceName());
            totalSaved += fetcher.fetchArticles(perSourceLimit).size();
        }
        System.out.printf("✅ All sources complete — Total Saved: %d%n", totalSaved);
    }
}

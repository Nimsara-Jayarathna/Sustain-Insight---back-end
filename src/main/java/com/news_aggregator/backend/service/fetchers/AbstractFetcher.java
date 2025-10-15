package com.news_aggregator.backend.service.fetchers;

import com.news_aggregator.backend.model.SourceFetchLog;
import com.news_aggregator.backend.repository.SourceFetchLogRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
public abstract class AbstractFetcher {

    private final SourceFetchLogRepository logRepo;

    protected OffsetDateTime getLastFetched(String source) {
        return logRepo.findBySourceName(source)
                .map(SourceFetchLog::getLastFetchedAt)
                .orElse(null);
    }

    protected void updateLastFetched(String source, OffsetDateTime latest, int totalFetched) {
        SourceFetchLog log = logRepo.findBySourceName(source)
                .orElseGet(() -> {
                    SourceFetchLog s = new SourceFetchLog();
                    s.setSourceName(source);
                    return s;
                });
        log.setLastFetchedAt(latest);
        log.setLastRun(OffsetDateTime.now());
        log.setTotalFetched(totalFetched);
        logRepo.save(log);
    }
}

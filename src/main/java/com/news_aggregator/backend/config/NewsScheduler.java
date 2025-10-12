package com.news_aggregator.backend.config;

import com.news_aggregator.backend.repository.RawArticleRepository;
import com.news_aggregator.backend.service.ArticleOrchestrationService;
import com.news_aggregator.backend.service.RawNewsFetcherService;
import com.news_aggregator.backend.service.SynthesisState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Lazy;

@Component
@EnableScheduling
public class NewsScheduler {

    private final RawNewsFetcherService rawNewsFetcherService;
    private final ArticleOrchestrationService articleOrchestrationService;
    private final RawArticleRepository rawArticleRepository;
    private final SynthesisState synthesisState;
    private final int fetchingEnabled;
    private final int scheduledLimit;
    private final int synthesisThreshold;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public NewsScheduler(RawNewsFetcherService rawNewsFetcherService,
                         @Lazy ArticleOrchestrationService articleOrchestrationService,
                         RawArticleRepository rawArticleRepository,
                         SynthesisState synthesisState,
                         @Value("${fetching.enabled:1}") int fetchingEnabled,
                         @Value("${fetching.scheduled.limit:10}") int scheduledLimit,
                         @Value("${synthesis.trigger.threshold:100}") int synthesisThreshold) {
        this.rawNewsFetcherService = rawNewsFetcherService;
        this.articleOrchestrationService = articleOrchestrationService;
        this.rawArticleRepository = rawArticleRepository;
        this.synthesisState = synthesisState;
        this.fetchingEnabled = fetchingEnabled;
        this.scheduledLimit = scheduledLimit;
        this.synthesisThreshold = synthesisThreshold;
    }

    @Scheduled(fixedDelayString = "${fetching.delay}", initialDelay = 10000)
    public void scheduledLogic() {
        String time = LocalDateTime.now().format(TIME_FMT);

        if (fetchingEnabled == 0) {
            return;
        }

        if (synthesisState.isSynthesisInProgress()) {
            System.out.printf("[%s] ⏸ Synthesis is in progress. Skipping this cycle.%n", time);
            return;
        }

        try {
            long unprocessedCount = rawArticleRepository.countByProcessedFalse();
            System.out.printf("[%s] 🔎 Found %d unprocessed articles.%n", time, unprocessedCount);

            if (unprocessedCount >= synthesisThreshold) {
                System.out.printf("[%s] 🔥 Threshold of %d reached. Triggering synthesis...%n", time, synthesisThreshold);
                articleOrchestrationService.orchestrateArticleProcessing();
            } else {
                System.out.printf("[%s] 🚀 Fetching new articles...%n", time);
                rawNewsFetcherService.fetchFromAllSources(scheduledLimit);
                System.out.printf("[%s] ✅ Fetching completed.%n", LocalDateTime.now().format(TIME_FMT));
            }

        } catch (Exception e) {
            System.err.printf("[%s] ⚠️ Scheduled task failed: %s%n",
                    LocalDateTime.now().format(TIME_FMT), e.getMessage());
            e.printStackTrace();
        }
    }
}

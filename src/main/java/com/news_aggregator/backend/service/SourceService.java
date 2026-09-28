package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.model.Source;
import com.news_aggregator.backend.repository.SourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class SourceService {

    private final SourceRepository sourceRepository;

    public SourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllAsMap() {
        return sourceRepository.findAll().stream()
                .sorted(Comparator.comparing(Source::getName, String.CASE_INSENSITIVE_ORDER))
                .map(source -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", source.getId());
                    map.put("name", source.getName());
                    return map;
                })
                .toList();
    }

    /**
     * Ensures every publisher/provider seen in the current raw batch exists in
     * the sources reference table before the AI synthesis prompt is created.
     */
    @Transactional
    public void synchronizeFromRawArticles(List<RawArticle> rawArticles) {
        if (rawArticles == null || rawArticles.isEmpty()) {
            return;
        }

        Map<String, String> uniqueNames = new LinkedHashMap<>();
        for (RawArticle rawArticle : rawArticles) {
            if (rawArticle == null) {
                continue;
            }
            String cleaned = cleanName(bestSourceName(rawArticle));
            if (!cleaned.isBlank()) {
                uniqueNames.putIfAbsent(cleaned.toLowerCase(Locale.ROOT), cleaned);
            }
        }

        uniqueNames.values().forEach(this::getOrCreate);
    }

    @Transactional
    public synchronized Source getOrCreate(String candidateName) {
        String name = cleanName(candidateName);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Source name must not be blank");
        }

        return sourceRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Source source = new Source();
                    source.setName(name);
                    return sourceRepository.save(source);
                });
    }

    private String bestSourceName(RawArticle rawArticle) {
        if (rawArticle.getSourceName() != null && !rawArticle.getSourceName().isBlank()) {
            return rawArticle.getSourceName();
        }
        return rawArticle.getApiSource();
    }

    private String cleanName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}

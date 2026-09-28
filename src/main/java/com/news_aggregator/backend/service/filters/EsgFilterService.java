package com.news_aggregator.backend.service.filters;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class EsgFilterService {

    // ✅ Core ESG-related keywords and phrases
    private static final List<String> ESG_KEYWORDS = Arrays.asList(
            "esg",
            "sustainability",
            "sustainable",
            "green finance",
            "climate",
            "carbon",
            "net zero",
            "renewable",
            "solar",
            "wind",
            "hydro",
            "geothermal",
            "battery",
            "electric vehicle",
            "clean energy",
            "environment",
            "environmental",
            "biodiversity",
            "pollution",
            "waste",
            "recycling",
            "eco-friendly",
            "sustainable living",
            "green technology",
            "social governance",
            "responsible investing",
            "global warming",
            "resilience"
    );

    /**
     * Determines whether the given text fields (title, description, content)
     * are relevant to ESG or sustainability topics.
     *
     * @param title       article title
     * @param description article summary/description
     * @param content     article full content
     * @return true if article is ESG-related
     */
    public boolean isEsgRelevant(String title, String description, String content) {
        String text = ((title == null ? "" : title) + " " +
                       (description == null ? "" : description) + " " +
                       (content == null ? "" : content)).toLowerCase();

        for (String keyword : ESG_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

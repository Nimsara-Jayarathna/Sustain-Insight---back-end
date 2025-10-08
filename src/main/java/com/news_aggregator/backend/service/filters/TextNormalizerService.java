package com.news_aggregator.backend.service.filters;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

@Service
public class TextNormalizerService {

    /**
     * 🧼 Removes HTML tags and extra whitespace from a string.
     * Works best for Guardian articles that come with embedded HTML.
     *
     * @param html the HTML text to clean
     * @return cleaned plain text
     */
    public String cleanHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }

        // Parse and strip HTML tags using Jsoup
        String text = Jsoup.parse(html).text();
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * ✂️ Removes the "[+123 chars]" style truncation markers
     * used by NewsAPI in content fields.
     *
     * @param content the raw article content
     * @return truncated marker removed
     */
    public String removeTruncationMarker(String content) {
        if (content == null) return null;
        return content.replaceAll("\\[\\+\\d+ chars\\]", "").trim();
    }

    /**
     * 🧩 Combines cleanup for HTML and truncation markers.
     * This can be used universally across all API fetchers.
     */
    public String normalize(String text) {
        if (text == null) return null;
        text = cleanHtml(text);
        text = removeTruncationMarker(text);
        return text;
    }
}

package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.Source;
import com.news_aggregator.backend.repository.ArticleRepository;
import com.news_aggregator.backend.repository.CategoryRepository;
import com.news_aggregator.backend.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final RestTemplate restTemplate;
    private final ArticleRepository articleRepository;
    private final SourceRepository sourceRepository;
    private final CategoryRepository categoryRepository;

    @Value("${newsapi.url}")
    private String baseUrl;

    @Value("${newsapi.apiKey}")
    private String apiKey;

    @Value("${newsapi.query}")
    private String query;

    @Value("${newsapi.sortBy}")
    private String sortBy;

    @Value("${newsapi.pageSize}")
    private int defaultPageSize;

    @Value("${newsapi.language}")
    private String language;

    @Transactional
    public void fetchAndSaveArticles(int targetNewArticles) {
        int savedCount = 0;
        int skippedCount = 0;
        int page = 1;
        int maxPages = 10; // safety cap

        while (savedCount < targetNewArticles && page <= maxPages) {
            String url = String.format(
                    "%s/everything?q=%s&sortBy=%s&pageSize=%d&page=%d&language=%s&apiKey=%s",
                    baseUrl, query, sortBy, defaultPageSize, page, language, apiKey
            );

            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(url, HttpMethod.GET, null,
                            new ParameterizedTypeReference<>() {});

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.out.println("⚠️ API request failed on page " + page);
                break;
            }

            Object articlesObj = response.getBody().get("articles");
            if (!(articlesObj instanceof List<?> list) || list.isEmpty()) {
                System.out.println("ℹ️ No more articles available at page " + page);
                break;
            }

            for (Object obj : list) {
                if (!(obj instanceof Map<?, ?> item)) continue;

                Article article = new Article();
                article.setTitle(truncate((String) item.get("title"), 500));
                article.setSummary(truncate((String) item.get("description"), 1000));
                article.setContent(truncate((String) item.get("content"), 5000));
                article.setImageUrl(truncate((String) item.get("urlToImage"), 1000));

                String publishedAtStr = (String) item.get("publishedAt");
                if (publishedAtStr != null) {
                    article.setPublishedAt(OffsetDateTime.parse(publishedAtStr));
                }

                OffsetDateTime now = OffsetDateTime.now();
                article.setCreatedAt(now);
                article.setUpdatedAt(now);

                // 🔹 Normalize + check for source, but don’t create yet
                Source source = null;
                Object src = item.get("source");

                if (src instanceof Map<?, ?> srcMap) {
                    String rawName = (String) srcMap.get("name");
                    String normalized = normalizeSourceName(rawName);

                    if (normalized != null && !normalized.isBlank()) {
                        source = sourceRepository.findByName(normalized).orElse(null);

                        if (source == null) {
                            System.out.println("ℹ️ Source not yet in DB: " + normalized +
                                               " (will only create if article is saved)");
                        } else {
                            System.out.println("🔗 Matched existing source: " + normalized);
                        }
                    }
                }

                // 🚨 Skip if no source info at all
                if (source == null && (src == null || ((Map<?, ?>) src).get("name") == null)) {
                    skippedCount++;
                    System.out.println("⏭️ Skipped (no valid source field): " + article.getTitle());
                    continue;
                }

                // 🚨 Duplicate check
                if (source != null &&
                        article.getTitle() != null &&
                        article.getPublishedAt() != null &&
                        articleRepository.existsByTitleAndPublishedAtAndSources_Name(
                                article.getTitle(),
                                article.getPublishedAt(),
                                source.getName())) {
                    skippedCount++;
                    System.out.println("⏭️ Skipped duplicate: " + article.getTitle());
                    continue;
                }

                // 🚨 ESG filter
                if (!passesEsgFilter(article)) {
                    skippedCount++;
                    System.out.println("⏭️ Skipped (failed ESG filter): " + article.getTitle());
                    continue;
                }

                // ✅ Only now → create source if still missing
                if (source == null && src instanceof Map<?, ?> srcMap) {
                    String rawName = (String) srcMap.get("name");
                    String normalized = normalizeSourceName(rawName);
                    if (normalized != null && !normalized.isBlank()) {
                        try {
                            Source newSource = new Source();
                            newSource.setName(normalized);
                            source = sourceRepository.save(newSource);
                            System.out.println("🆕 Created new source (with article): " + normalized);
                        } catch (Exception e) {
                            source = sourceRepository.findByName(normalized).orElse(null);
                            System.out.println("⚠️ Race condition: source already created " + normalized);
                        }
                    }
                }

                // ✅ Maintain both sides of relation
                if (source != null) {
                    article.getSources().add(source);
                    source.getArticles().add(article);
                }

                // 🔹 Assign category
                String categoryName = detectCategory(article.getTitle() + " " + article.getSummary());
                categoryRepository.findByName(categoryName)
                        .ifPresent(article.getCategories()::add);

                articleRepository.save(article);
                savedCount++;
                System.out.println("✅ Saved: " + article.getTitle());

                if (savedCount >= targetNewArticles) break;
            }

            page++;
        }

        System.out.println("📊 Fetch complete — Saved: " + savedCount + " | Skipped: " + skippedCount);
    }

    // --- ESG Filter ---
    private boolean passesEsgFilter(Article article) {
        String text = (article.getTitle() + " " + article.getSummary() + " " + article.getContent())
                .toLowerCase();

        return text.contains("esg")
                || text.contains("sustainability")
                || text.contains("sustainable")
                || text.contains("green finance")
                || text.contains("climate")
                || text.contains("carbon")
                || text.contains("net zero")
                || text.contains("renewable")
                || text.contains("solar")
                || text.contains("wind")
                || text.contains("hydro")
                || text.contains("geothermal")
                || text.contains("battery")
                || text.contains("ev ")
                || text.contains("electric vehicle")
                || text.contains("clean energy")
                || text.contains("environmental")
                || text.contains("biodiversity")
                || text.contains("deforestation")
                || text.contains("pollution")
                || text.contains("waste")
                || text.contains("recycling")
                || text.contains("circular economy")
                || text.contains("eco-friendly")
                || text.contains("sustainable living")
                || text.contains("green technology")
                || text.contains("corporate responsibility")
                || text.contains("social governance")
                || text.contains("responsible investing")
                || text.contains("greenwashing")
                || text.contains("global warming")
                || text.contains("emissions")
                || text.contains("carbon credits")
                || text.contains("carbon trading")
                || text.contains("carbon offset")
                || text.contains("adaptation")
                || text.contains("resilience");
    }

    // --- Helpers ---
    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max - 3) + "..." : value;
    }

    private String normalizeSourceName(String rawName) {
        if (rawName == null) return null;

        String name = rawName.trim();

        return switch (name) {
            // --- Mainstream ---
            case "NY Times", "New York Times", "NYTimes", "NYTimes.com", "The NY Times" ->
                    "The New York Times";
            case "BBC", "BBC News", "BBC.com", "BBC UK", "BBC World" ->
                    "BBC News";
            case "CNN International", "CNN.com", "CNN", "CNN US", "CNN World" ->
                    "CNN";
            case "Guardian", "The Guardian UK", "Guardian News", "The Guardian" ->
                    "The Guardian";
            case "Reuters", "Reuters.com", "Reuters News", "Thomson Reuters" ->
                    "Reuters";

            // --- Sustainability & Science ---
            case "NatGeo", "National Geographic Magazine", "NatGeo.com", "National Geographic" ->
                    "National Geographic";
            case "Green Biz", "GreenBiz.com", "GreenBiz", "Green Business" ->
                    "GreenBiz";
            case "Sustainability Times", "Sustainability Times Online" ->
                    "Sustainability Times";
            case "EcoBusiness", "Eco Business", "Eco-Business" ->
                    "Eco-Business";
            case "Yale E360", "Yale Environment", "Yale Env 360", "Yale Environment 360" ->
                    "Yale Environment 360";

            // --- Financial / ESG ---
            case "Bloomberg", "Bloomberg.com", "Bloomberg News" ->
                    "Bloomberg";
            case "Financial Times", "FT.com", "The Financial Times", "FT" ->
                    "Financial Times";
            case "Forbes", "Forbes.com", "Forbes Magazine" ->
                    "Forbes";

            // --- Tech & Business ---
            case "TechCrunch", "Tech Crunch", "TechCrunch.com" ->
                    "TechCrunch";
            case "Wired", "Wired.com", "WIRED Magazine" ->
                    "Wired";

            default -> name; // fallback: keep raw
        };
    }

    private String detectCategory(String text) {
        if (text == null) return "General Sustainability";
        String lower = text.toLowerCase();

        if (lower.contains("solar") || lower.contains("wind") || lower.contains("renewable") || lower.contains("hydrogen"))
            return "Renewable Energy";
        else if (lower.contains("climate") || lower.contains("warming") || lower.contains("carbon") || lower.contains("emissions"))
            return "Climate Change";
        else if (lower.contains("esg") || lower.contains("governance") || lower.contains("finance") || lower.contains("responsibility"))
            return "ESG & Green Finance";
        else if (lower.contains("pollution") || lower.contains("waste") || lower.contains("plastic") || lower.contains("recycling"))
            return "Pollution & Waste";
        else if (lower.contains("ev") || lower.contains("battery") || lower.contains("tech") || lower.contains("green technology"))
            return "Green Technology";
        else if (lower.contains("farm") || lower.contains("agriculture") || lower.contains("food") || lower.contains("water"))
            return "Agriculture & Food";
        else if (lower.contains("lifestyle") || lower.contains("fashion") || lower.contains("housing") || lower.contains("eco"))
            return "Sustainable Living";
        else
            return "General Sustainability";
    }
}

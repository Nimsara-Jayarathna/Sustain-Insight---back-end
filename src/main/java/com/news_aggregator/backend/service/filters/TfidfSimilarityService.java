package com.news_aggregator.backend.service.filters;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TfidfSimilarityService {

    private final Analyzer analyzer = new EnglishAnalyzer();

    /** 🔹 Convert article text into normalized tokens */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        try (var ts = analyzer.tokenStream("field", new StringReader(text == null ? "" : text))) {
            ts.reset();
            while (ts.incrementToken()) {
                tokens.add(ts.getAttribute(CharTermAttribute.class).toString());
            }
            ts.end();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tokens;
    }

    /** 🔹 Build simple term frequency vector */
    private Map<String, Double> termFrequency(List<String> tokens) {
        Map<String, Double> tf = new HashMap<>();
        for (String t : tokens) tf.put(t, tf.getOrDefault(t, 0.0) + 1.0);
        double total = tokens.size();
        tf.replaceAll((k, v) -> v / total);
        return tf;
    }

    /** 🔹 Cosine similarity between two term frequency vectors */
    public double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2) {
        Set<String> all = new HashSet<>(vec1.keySet());
        all.addAll(vec2.keySet());
        double dot = 0, mag1 = 0, mag2 = 0;
        for (String t : all) {
            double v1 = vec1.getOrDefault(t, 0.0);
            double v2 = vec2.getOrDefault(t, 0.0);
            dot += v1 * v2;
            mag1 += v1 * v1;
            mag2 += v2 * v2;
        }
        return (mag1 == 0 || mag2 == 0) ? 0 : dot / (Math.sqrt(mag1) * Math.sqrt(mag2));
    }

    /** 🔹 Compute similarity using title + description + content */
    private double computeArticleSimilarity(String title1, String desc1, String content1,
                                            String title2, String desc2, String content2) {

        String full1 = String.join(" ",
                Optional.ofNullable(title1).orElse(""),
                Optional.ofNullable(desc1).orElse(""),
                Optional.ofNullable(content1).orElse(""));

        String full2 = String.join(" ",
                Optional.ofNullable(title2).orElse(""),
                Optional.ofNullable(desc2).orElse(""),
                Optional.ofNullable(content2).orElse(""));

        var tokens1 = tokenize(full1);
        var tokens2 = tokenize(full2);

        var tf1 = termFrequency(tokens1);
        var tf2 = termFrequency(tokens2);

        return cosineSimilarity(tf1, tf2);
    }

    /**
     * 🔹 Compare all articles pairwise and return only minimal info:
     * id1, id2, similarity score
     */
    public List<SimilarityResult> findSimilarArticles(List<ArticleMinimal> articles, double threshold) {
        List<SimilarityResult> results = new ArrayList<>();
        int total = articles.size();
        System.out.printf("🔍 Starting TF-IDF comparisons for %d articles...%n", total);

        for (int i = 0; i < total; i++) {
            for (int j = i + 1; j < total; j++) {
                var a1 = articles.get(i);
                var a2 = articles.get(j);

                double score = computeArticleSimilarity(
                        a1.title(), a1.description(), a1.content(),
                        a2.title(), a2.description(), a2.content()
                );

                if (score >= threshold) {
                    results.add(new SimilarityResult(a1.id(), a2.id(), score));
                }
            }
        }

        System.out.printf("✅ Completed comparisons: %d pairs above threshold %.2f%n",
                results.size(), threshold);
        return results;
    }

    /** 🔹 Record classes (lightweight only) */
    public static record ArticleMinimal(Long id, String title, String description, String content) {}
    public static record SimilarityResult(Long id1, Long id2, double similarity) {}
}

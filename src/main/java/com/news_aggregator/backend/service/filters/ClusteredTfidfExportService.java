package com.news_aggregator.backend.service.filters;

import com.news_aggregator.backend.model.RawArticle;
import com.news_aggregator.backend.repository.RawArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds a structured export containing only clusters of related articles
 * (articles that have at least one strong TF-IDF relationship).
 *
 * Each cluster groups mutually similar articles together
 * to prepare them for AI-based deduplication/synthesis.
 */
@Service
@RequiredArgsConstructor
public class ClusteredTfidfExportService {

    private final RawArticleRepository rawRepo;

    /**
     * Builds a JSON-like structure with only related articles.
     *
     * @param tfidfPairs list of {id1, id2, similarity} maps
     * @param threshold  minimum similarity score to include a relation
     * @return Map containing meta + clusters for AI input
     */
    public Map<String, Object> buildClusteredExport(List<Map<String, Object>> tfidfPairs, double threshold) {
        // 🧩 Load all articles from DB
        List<RawArticle> allArticles = rawRepo.findAll();
        Map<Long, RawArticle> articleMap = allArticles.stream()
                .collect(Collectors.toMap(RawArticle::getId, a -> a));

        // 🧠 Step 1: Build graph of strong similarities
        Map<Long, Set<Long>> graph = new HashMap<>();
        List<Map<String, Object>> validPairs = new ArrayList<>();

        for (Map<String, Object> pair : tfidfPairs) {
            double sim = Double.parseDouble(pair.get("similarity").toString());
            if (sim < threshold) continue;

            Long id1 = Long.parseLong(pair.get("id1").toString());
            Long id2 = Long.parseLong(pair.get("id2").toString());

            if (!articleMap.containsKey(id1) || !articleMap.containsKey(id2))
                continue; // skip unknown IDs

            Map<String, Object> validPair = new HashMap<>();
            validPair.put("id1", id1);
            validPair.put("id2", id2);
            validPair.put("similarity", sim);
            validPairs.add(validPair);

            graph.computeIfAbsent(id1, k -> new HashSet<>()).add(id2);
            graph.computeIfAbsent(id2, k -> new HashSet<>()).add(id1);
        }

        // 🕸 Step 2: Find connected components (clusters)
        Set<Long> visited = new HashSet<>();
        List<Map<String, Object>> clusters = new ArrayList<>();

        for (Long id : graph.keySet()) {
            if (visited.contains(id)) continue;

            // DFS/BFS to find all connected nodes
            Set<Long> clusterIds = new HashSet<>();
            Deque<Long> stack = new ArrayDeque<>(List.of(id));

            while (!stack.isEmpty()) {
                Long current = stack.pop();
                if (!visited.add(current)) continue;
                clusterIds.add(current);
                for (Long nbr : graph.getOrDefault(current, Set.of())) {
                    if (!visited.contains(nbr)) stack.push(nbr);
                }
            }

            // Build cluster article details
            List<Map<String, Object>> articles = clusterIds.stream()
                    .map(articleMap::get)
                    .filter(Objects::nonNull)
                    .map(a -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", a.getId());
                        map.put("api_source", a.getApiSource());
                        map.put("source_name", a.getSourceName());
                        map.put("title", a.getTitle());
                        map.put("description", a.getDescription());
                        map.put("content", a.getContent());
                        map.put("url", a.getUrl());
                        map.put("image_url", a.getImageUrl());
                        map.put("published_at", a.getPublishedAt());
                        map.put("fetched_at", a.getFetchedAt());
                        return map;
                    })
                    .collect(Collectors.toList());

            // Extract relevant relations for this cluster only
            List<Map<String, Object>> relations = validPairs.stream()
                    .filter(p -> clusterIds.contains(Long.parseLong(p.get("id1").toString())) &&
                                 clusterIds.contains(Long.parseLong(p.get("id2").toString())))
                    .collect(Collectors.toList());

            // Build cluster map manually (avoid Map.of())
            Map<String, Object> cluster = new LinkedHashMap<>();
            cluster.put("cluster_id", "cluster_" + (clusters.size() + 1));
            cluster.put("primary_article_id", clusterIds.iterator().next());
            cluster.put("related_article_ids", new ArrayList<>(clusterIds));
            cluster.put("articles", articles);
            cluster.put("relations", relations);
            clusters.add(cluster);
        }

        // 🧾 Step 3: Meta Information
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("source", "raw_articles");
        meta.put("total_clusters", clusters.size());
        meta.put("similarity_threshold", threshold);
        meta.put("generated_at", OffsetDateTime.now().toString());

        // 🎯 Step 4: Final JSON structure
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("meta", meta);
        export.put("clusters", clusters);

        return export;
    }
}

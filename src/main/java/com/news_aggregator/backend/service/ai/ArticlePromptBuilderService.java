package com.news_aggregator.backend.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArticlePromptBuilderService {

    private final ObjectMapper mapper;

    public String buildEngineeredPrompt(
            List<Map<String, Object>> clusters,
            List<Map<String, Object>> availableCategories,
            List<Map<String, Object>> availableSources
    ) {
        try {
            String prompt = "You are a sophisticated AI News Analyst. Your task is to process a list of article clusters. "
                    + "For each cluster, you must identify the primary article, summarize the content of the other articles, and merge it into the primary article. "
                    + "You must then generate a new title and summary for the primary article, and assign categories and sources. "
                    + "Follow these steps for each cluster:\n"
                    + "1.  Identify the primary article using the `primary_article_id`.\n"
                    + "2.  Summarize the content of all other articles in the `articles` array.\n"
                    + "3.  Merge the summarized content into the `content` of the primary article. Also, update the `title` and `summary` of the primary article to reflect the new content.\n"
                    + "4.  Keep the `id`, `api_source`, `url`, `image_url`, and `published_at` of the original primary article.\n"
                    + "5.  Analyze the new content and assign relevant category IDs from the `available_categories`. If no specific category matches, default to `[8]` (General Sustainability).\n"
                    + "6.  Assign relevant source IDs from the `available_sources`. If no source is found, you can assign a random one.\n"
                    + "Finally, you must call the `article_list_generator` tool with the list of processed primary articles.\n\n"
                    + "Available Categories:\n" + mapper.writeValueAsString(availableCategories) + "\n"
                    + "Available Sources:\n" + mapper.writeValueAsString(availableSources) + "\n"
                    + "Article Clusters:\n" + mapper.writeValueAsString(clusters);

            Map<String, Object> userPart = new HashMap<>();
            userPart.put("role", "user");
            userPart.put("parts", List.of(Map.of("text", prompt)));

            Map<String, Object> toolDecl = Map.of(
                    "function_declarations", List.of(Map.of(
                            "name", "article_list_generator",
                            "description", "Generates a list of processed articles.",
                            "parameters", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "articles", Map.of(
                                                    "type", "ARRAY",
                                                    "items", Map.of(
                                                            "type", "OBJECT",
                                                            "properties", Map.of(
                                                                    "id", Map.of("type", "INTEGER"),
                                                                    "api_source", Map.of("type", "STRING"),
                                                                    "title", Map.of("type", "STRING"),
                                                                    "summary", Map.of("type", "STRING"),
                                                                    "content", Map.of("type", "STRING", "description", "The full text content of the new article, composed in well-structured paragraphs. The content should be limited to 600 characters."),
                                                                    "url", Map.of("type", "STRING"),
                                                                    "image_url", Map.of("type", "STRING"),
                                                                    "published_at", Map.of("type", "STRING", "description", "The publication timestamp in ISO 8601 format (e.g., '2025-10-09T12:00:00Z')."),
                                                                    "category_ids", Map.of("type", "ARRAY", "items", Map.of("type", "INTEGER")),
                                                                    "source_ids", Map.of("type", "ARRAY", "items", Map.of("type", "INTEGER"))
                                                            )
                                                    )
                                            )
                                    )
                            )
                    ))
            );

            Map<String, Object> root = new HashMap<>();
            root.put("contents", List.of(userPart));
            root.put("tools", List.of(toolDecl));
            root.put("tool_config", Map.of(
                    "function_calling_config",
                    Map.of("mode", "ANY", "allowed_function_names", List.of("article_list_generator")
            )));

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build engineered prompt JSON", e);
        }
    }
}
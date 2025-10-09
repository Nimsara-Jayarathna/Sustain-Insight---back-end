package com.news_aggregator.backend.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class ArticleSynthesisService {

    private final String geminiApiKey;
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent";

    private final ObjectMapper mapper = new ObjectMapper();

    public ArticleSynthesisService(@Value("${gemini.api.key}") String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    /**
     * 🔹 Sends a JSON cluster prompt to Gemini and returns clean extracted JSON content.
     */
    public String generateUnifiedArticle(String engineeredPromptJson) {
        try {
            System.out.println("🤖 Sending engineered JSON to Gemini...");
            String requestUrl = GEMINI_URL + "?key=" + geminiApiKey;

            // ✅ Build request body
            Map<String, Object> body = mapper.readValue(engineeredPromptJson, new TypeReference<Map<String, Object>>() {});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(
                    new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                        setConnectTimeout((int) Duration.ofSeconds(600).toMillis());
                        setReadTimeout((int) Duration.ofSeconds(600).toMillis());
                    }}
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    requestUrl, HttpMethod.POST, entity, String.class);

            System.out.println("✅ Gemini responded with: " + response.getStatusCode());

            String responseBody = response.getBody();
            System.out.println("📥 Raw Gemini response: " + responseBody);

            // 🧹 Extract clean JSON from Gemini response
            String cleanJson = extractGeminiText(responseBody);
            System.out.println("🧩 Clean extracted JSON ready for DB insert:");
            System.out.println(cleanJson);

            return cleanJson;

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Gemini API call failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 🔹 Extracts inner 'text' field from Gemini's candidates -> content -> parts array.
     */
    private String extractGeminiText(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode functionCall = candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("functionCall");

                if (!functionCall.isMissingNode() && functionCall.path("name").asText().equals("article_list_generator")) {
                    return functionCall.path("args").path("articles").toString();
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to parse Gemini response: " + e.getMessage());
        }
        return "[]"; // Return an empty array string if something goes wrong
    }
}
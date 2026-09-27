package com.news_aggregator.backend.email.template;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Owns Sustain Insight email rendering. Provider adapters receive fully rendered HTML
 * and never know about templates or business variables.
 */
@Service
public class EmailTemplateService {

    private final TemplateEngine templateEngine;
    private final String brandName;
    private final String frontendUrl;

    public EmailTemplateService(
            TemplateEngine templateEngine,
            @Value("${app.brand.name:Sustain Insight}") String brandName,
            @Value("${app.frontend-url:}") String frontendUrl) {
        this.templateEngine = templateEngine;
        this.brandName = brandName;
        this.frontendUrl = normalizeBaseUrl(frontendUrl);
    }

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        Map<String, Object> merged = baseVariables();
        if (variables != null) {
            merged.putAll(variables);
        }
        context.setVariables(merged);
        return templateEngine.process("email/" + templateName, context);
    }

    public String getBrandName() {
        return brandName;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    private Map<String, Object> baseVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("brandName", brandName);
        variables.put("currentYear", Year.now().getValue());
        variables.put("frontendUrl", frontendUrl.isBlank() ? "#" : frontendUrl);
        variables.put(
                "forgotPasswordUrl",
                frontendUrl.isBlank() ? "#" : frontendUrl + "/forgot-password");
        return variables;
    }

    private String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        while (trimmed.endsWith("/") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}

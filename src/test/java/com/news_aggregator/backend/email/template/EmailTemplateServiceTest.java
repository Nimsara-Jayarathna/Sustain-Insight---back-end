package com.news_aggregator.backend.email.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class EmailTemplateServiceTest {

    private EmailTemplateService service;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        service = new EmailTemplateService(engine, "Sustain Insight", "https://staging.example.com/");
    }

    @Test
    void rendersVerificationTemplateWithEscapedVariablesAndNormalizedUrl() {
        String html = service.render(
                "account-verification",
                Map.of(
                        "firstName", "Nimal",
                        "verificationLink", "https://staging.example.com/verify?token=abc"));

        assertThat(html).contains("Nimal");
        assertThat(html).contains("Sustain Insight");
        assertThat(html).contains("https://staging.example.com/verify?token=abc");
        assertThat(service.getFrontendUrl()).isEqualTo("https://staging.example.com");
    }

    @Test
    void rendersOtpTemplate() {
        String html = service.render("current-email-otp", Map.of("otp", "123456"));
        assertThat(html).contains("123456");
    }
}

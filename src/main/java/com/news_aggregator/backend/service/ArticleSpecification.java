package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.Category;
import com.news_aggregator.backend.model.Source;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ArticleSpecification {

    public static Specification<Article> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")),
                            likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")),
                            likePattern)
            );
        };
    }

    public static Specification<Article> hasCategories(List<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            query.distinct(true);
            Join<Article, Category> categoryJoin = root.join("categories");
            return categoryJoin.get("id").in(categoryIds);
        };
    }

    public static Specification<Article> hasSources(List<Long> sourceIds) {
        return (root, query, criteriaBuilder) -> {
            if (sourceIds == null || sourceIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            query.distinct(true);
            Join<Article, Source> sourceJoin = root.join("sources");
            return sourceJoin.get("id").in(sourceIds);
        };
    }

    public static Specification<Article> hasDate(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.function("DATE", LocalDate.class, root.get("publishedAt")),
                    date
            );
        };
    }

    public static Specification<Article> isWithinLast24Hours() {
        return (root, query, criteriaBuilder) -> {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime twentyFourHoursAgo = now.minus(24, ChronoUnit.HOURS);
            return criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("publishedAt"), twentyFourHoursAgo),
                    criteriaBuilder.lessThanOrEqualTo(root.get("publishedAt"), now)
            );
        };
    }
}

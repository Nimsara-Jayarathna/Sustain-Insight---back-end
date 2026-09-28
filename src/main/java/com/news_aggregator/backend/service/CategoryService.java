package com.news_aggregator.backend.service;

import com.news_aggregator.backend.model.Category;
import com.news_aggregator.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    public static final String DEFAULT_CATEGORY_NAME = "General Sustainability";

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllAsMap() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(category -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", category.getId());
                    map.put("name", category.getName());
                    return map;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Category getDefaultCategory() {
        return categoryRepository.findByNameIgnoreCase(DEFAULT_CATEGORY_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Required reference category '" + DEFAULT_CATEGORY_NAME
                                + "' is missing. Ensure Flyway migration V3 has been applied."));
    }
}

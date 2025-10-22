package com.news_aggregator.backend.controller;

import com.news_aggregator.backend.dto.SourceDto;
import com.news_aggregator.backend.repository.SourceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/sources")
public class SourceController {

    private final SourceRepository sourceRepository;

    public SourceController(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @GetMapping
    public List<SourceDto> getAllSources() {
        return sourceRepository.findAll().stream()
                .map(s -> new SourceDto(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }
}

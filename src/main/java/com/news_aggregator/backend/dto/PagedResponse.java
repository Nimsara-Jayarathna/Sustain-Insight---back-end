package com.news_aggregator.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;       // actual page of results
    private int currentPage;       // which page are we on
    private int totalPages;        // total number of pages
    private long totalElements;    // total articles matching query
    private int pageSize;          // size per page
}

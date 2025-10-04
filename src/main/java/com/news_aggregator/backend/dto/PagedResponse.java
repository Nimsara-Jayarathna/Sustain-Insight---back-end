package com.news_aggregator.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;       // actual page of results
    private int currentPage;       // current page (1-based for FE)
    private int pageSize;          // size per page
    private long totalElements;    // total number of items
    private int totalPages;        // total number of pages
    private boolean last;          // is this the last page?
}

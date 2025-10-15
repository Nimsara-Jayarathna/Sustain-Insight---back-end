package com.news_aggregator.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "source_fetch_log")
@Getter
@Setter
public class SourceFetchLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", unique = true, nullable = false)
    private String sourceName;

    @Column(name = "last_fetched_at")
    private OffsetDateTime lastFetchedAt;

    @Column(name = "total_fetched")
    private Integer totalFetched = 0;

    @Column(name = "last_run")
    private OffsetDateTime lastRun = OffsetDateTime.now();
}

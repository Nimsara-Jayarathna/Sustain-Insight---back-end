package com.news_aggregator.backend.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import jakarta.persistence.*;
import lombok.*;
import java.util.Map;    
import java.time.OffsetDateTime;

@Entity
@Table(name = "raw_articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String apiSource;          // e.g. "NewsAPI"

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String sourceName;

    private OffsetDateTime publishedAt;

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime fetchedAt = OffsetDateTime.now();

    @Column(columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Object> rawJson;
}

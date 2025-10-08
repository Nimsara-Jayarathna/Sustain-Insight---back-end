package com.news_aggregator.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "article_insights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleInsight {

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "insight_count", nullable = false)
    private Long insightCount = 0L;
}

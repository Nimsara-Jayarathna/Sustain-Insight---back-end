package com.news_aggregator.backend.model;

import java.io.Serializable;
import java.util.Objects;

public class InsightId implements Serializable {
    private Long user;
    private Long article;

    public InsightId() {}

    public InsightId(Long user, Long article) {
        this.user = user;
        this.article = article;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsightId)) return false;
        InsightId that = (InsightId) o;
        return Objects.equals(user, that.user) &&
               Objects.equals(article, that.article);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, article);
    }
}

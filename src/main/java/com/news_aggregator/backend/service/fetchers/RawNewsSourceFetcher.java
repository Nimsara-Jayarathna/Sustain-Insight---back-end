package com.news_aggregator.backend.service.fetchers;

import com.news_aggregator.backend.model.RawArticle;
import java.util.List;

public interface RawNewsSourceFetcher {
    String getSourceName();
    List<RawArticle> fetchArticles(int limit);
}

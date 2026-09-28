# Runtime hardening

This repository intentionally keeps `spring.jpa.open-in-view=false`. Lazy JPA relationships must therefore be consumed inside an explicit service transaction rather than being loaded from controllers after the persistence context has closed.

## Article read boundary

`Article.sources` and `Article.categories` remain `LAZY` to avoid turning every article query into a large eager join. `ArticleService` owns the transaction boundary for article-to-DTO mapping:

- `getLatestArticles(...)`
- `getAllArticles(...)`
- `getForYouFeed(...)`
- `getArticleDto(...)`

The insight endpoints use `getArticleDto(...)` after an insight mutation instead of mapping a detached `Article` in the controller. Do not move article DTO mapping back into controllers unless the repository query explicitly fetches every required lazy relationship.

## NewsAPI developer-plan pagination

NewsAPI developer accounts expose at most 100 results for a query. The fetcher therefore defaults to two pages of 50 results and stops immediately if NewsAPI returns `maximumResultsReached`.

Environment variables:

```dotenv
NEWS_API_PAGE_SIZE=50
NEWS_API_MAX_PAGES=2
NEWS_API_PAGE_DELAY_MS=2000
```

`NEWS_API_PAGE_SIZE` is clamped to the NewsAPI maximum of 100. `NEWS_API_MAX_PAGES` should normally stay at `2` for developer accounts. Paid plans can raise it without a code change.

Per-article `Not Relevant` output was removed. The fetcher now emits one page summary containing saved, duplicate and skipped counts, which keeps production logs useful.

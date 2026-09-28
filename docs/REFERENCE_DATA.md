# Reference data lifecycle

## Categories

Categories are controlled taxonomy/reference data. `V3__seed_reference_data.sql` seeds the required sustainability categories with `ON CONFLICT (name) DO NOTHING`, so existing data is preserved and missing rows are added automatically during Flyway startup.

The application never assumes a numeric category ID. `General Sustainability` is resolved by name and its actual database ID is supplied to the synthesis prompt as the fallback category.

## Sources

Sources are open-ended because NewsAPI and other providers can return publishers that were not known at deployment time. `SourceService` therefore discovers source names from `RawArticle.sourceName` (falling back to `apiSource`), normalizes whitespace, resolves names case-insensitively, and inserts a source only when it is missing.

Before synthesis, the current raw batch is synchronized into the `sources` table. The AI is instructed to use only IDs from that database-backed source list and never invent/randomly choose source IDs.

When saving synthesized output, category/source IDs are validated again. Missing/invalid category IDs fall back to `General Sustainability`; missing/invalid source IDs fall back to the original raw article publisher/provider and are resolved dynamically.

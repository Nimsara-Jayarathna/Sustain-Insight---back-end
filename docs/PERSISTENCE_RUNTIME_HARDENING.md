# Persistence and Runtime Hardening

This backend intentionally keeps `spring.jpa.open-in-view=false`. Lazy JPA relationships must therefore be consumed inside explicit persistence boundaries rather than relying on a web request keeping Hibernate sessions open.

## What was hardened

- Account profile reads load `preferredCategories` and `preferredSources` explicitly through `UserRepository.findProfileByEmail`.
- `/api/account/me` maps the user DTO inside a read-only transaction.
- Personalized article feeds use the profile-aware user query so preference collections are safe and deterministic.
- Bookmark listing is read-only transactional so `Bookmark.article.sources` and `Bookmark.article.categories` are initialized while the persistence context is valid.
- Bookmark creation is transactional.
- Refresh-token lookup now fetches both the associated user and session in the same query, and refresh orchestration is transactional.
- Password-reset token lookup explicitly fetches its user, avoiding detached-token failures during reset.
- User/article lazy collections use Hibernate batch fetching to reduce N+1 query pressure without changing them to `EAGER`.
- `hibernate.default_batch_fetch_size` is configurable with `HIBERNATE_DEFAULT_BATCH_FETCH_SIZE` (default 50).
- Email-change OTP generation now uses `SecureRandom` instead of `java.util.Random`.

## Design rule

Keep entity relationships `LAZY` by default. Fetch only the associations required by a use case and map entities to DTOs while inside a service/controller transaction. Do not globally switch collections to `EAGER` to hide `LazyInitializationException`.

## Environment

Recommended value:

```env
HIBERNATE_DEFAULT_BATCH_FETCH_SIZE=50
```

This controls Hibernate's lazy-association batch loading and does not change transaction semantics.

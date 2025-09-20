CREATE TABLE IF NOT EXISTS users (
    id bigserial PRIMARY KEY,
    first_name text NOT NULL,
    last_name text NOT NULL,
    job_title text,
    email text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS categories (
    id bigserial PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS sources (
    id bigserial PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS articles (
    id bigserial PRIMARY KEY,
    title text NOT NULL,
    summary text,
    content text,
    image_url text,
    published_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS article_categories (
    article_id bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    category_id bigint NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    PRIMARY KEY(article_id, category_id)
);

CREATE TABLE IF NOT EXISTS article_sources (
    article_id bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    source_id bigint NOT NULL REFERENCES sources(id) ON DELETE RESTRICT,
    PRIMARY KEY(article_id, source_id)
);

CREATE TABLE IF NOT EXISTS user_preferred_categories (
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id bigint NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY(user_id, category_id)
);

CREATE TABLE IF NOT EXISTS user_preferred_sources (
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_id bigint NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    PRIMARY KEY(user_id, source_id)
);

CREATE TABLE IF NOT EXISTS bookmarks (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_bookmark UNIQUE (user_id, article_id)
);

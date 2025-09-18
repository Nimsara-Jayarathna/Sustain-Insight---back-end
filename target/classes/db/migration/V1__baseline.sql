create table users (
                       id bigserial primary key,
                       first_name text not null,
                       last_name text not null,
                       job_title text,
                       email text not null unique,
                       password_hash text not null,
                       created_at timestamptz not null default now(),
                       updated_at timestamptz not null default now()
);

create table categories (
                            id bigserial primary key,
                            name text not null unique
);

create table sources (
                         id bigserial primary key,
                         name text not null unique
);

create table articles (
                          id bigserial primary key,
                          title text not null,
                          summary text,
                          content text,
                          image_url text,
                          published_at timestamptz,
                          created_at timestamptz not null default now(),
                          updated_at timestamptz not null default now()
);

create table article_categories (
                                    article_id bigint not null references articles(id) on delete cascade,
                                    category_id bigint not null references categories(id) on delete restrict,
                                    primary key(article_id, category_id)
);

create table article_sources (
                                 article_id bigint not null references articles(id) on delete cascade,
                                 source_id bigint not null references sources(id) on delete restrict,
                                 primary key(article_id, source_id)
);

create table user_preferred_categories (
                                           user_id bigint not null references users(id) on delete cascade,
                                           category_id bigint not null references categories(id) on delete cascade,
                                           primary key(user_id, category_id)
);

create table user_preferred_sources (
                                        user_id bigint not null references users(id) on delete cascade,
                                        source_id bigint not null references sources(id) on delete cascade,
                                        primary key(user_id, source_id)
);

create table bookmarks (
                           id bigserial primary key,
                           user_id bigint not null references users(id) on delete cascade,
                           article_id bigint not null references articles(id) on delete cascade,
                           created_at timestamptz not null default now(),
                           constraint uq_bookmark unique (user_id, article_id)
);

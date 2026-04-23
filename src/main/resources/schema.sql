-- WordBook v1 schema (PostgreSQL)
-- 수동 실행. application.yml 의 spring.sql.init.mode=never 이므로 앱 부팅 시 실행되지 않음.
-- RDS 에서 1회 실행 후 spring.jpa.hibernate.ddl-auto=validate 로 스키마 검증.

CREATE TABLE IF NOT EXISTS books (
    id         BIGSERIAL    PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    author     VARCHAR(100),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS words (
    id         BIGSERIAL    PRIMARY KEY,
    word       VARCHAR(100) NOT NULL,
    meaning    TEXT         NOT NULL,
    example    TEXT,
    status     VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    book_id    BIGINT       REFERENCES books(id) ON DELETE SET NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_words_book_id ON words(book_id);
CREATE INDEX IF NOT EXISTS idx_words_status  ON words(status);

-- 초기 책 5권
INSERT INTO books (title, author) VALUES
    ('Animal Farm',                      'George Orwell'),
    ('The Tale of Despereaux',           'Kate DiCamillo'),
    ('The Hunger Games',                 'Suzanne Collins'),
    ('The Great Gatsby',                 'F. Scott Fitzgerald'),
    ('Down and Out in Paris and London', 'George Orwell')
ON CONFLICT DO NOTHING;

-- E-Library Service schema.
-- SQLite has no native date/time type; timestamps are stored as ISO-8601 UTC text.

CREATE TABLE users (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE books (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    title            TEXT    NOT NULL,
    author           TEXT    NOT NULL,
    isbn             TEXT    NOT NULL,
    description      TEXT,
    total_copies     INTEGER NOT NULL,
    available_copies INTEGER NOT NULL,
    created_at       TEXT    NOT NULL,
    CONSTRAINT uq_books_isbn UNIQUE (isbn),
    CONSTRAINT ck_books_total_copies CHECK (total_copies >= 0),
    CONSTRAINT ck_books_available_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

CREATE TABLE loans (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id     INTEGER NOT NULL,
    user_id     INTEGER NOT NULL,
    borrowed_at TEXT    NOT NULL,
    returned_at TEXT,
    CONSTRAINT fk_loans_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT fk_loans_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Supports the "current loans" query, which only ever looks at not-yet-returned rows.
CREATE INDEX idx_loans_user_active ON loans (user_id) WHERE returned_at IS NULL;

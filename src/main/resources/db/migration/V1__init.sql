-- ====================================================================
-- Лабораторная работа № 4 — schema init (миграция V1)
-- PostgreSQL 17, схема public
-- ====================================================================

-- Авторы
CREATE TABLE authors (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Книги
CREATE TABLE books (
    isbn   VARCHAR(13) PRIMARY KEY,
    title  VARCHAR(255) NOT NULL,
    year   INT          NOT NULL CHECK (year >= 1450),
    copies INT          NOT NULL CHECK (copies >= 1)
);

-- Связь многие-ко-многим: книга <-> автор
CREATE TABLE book_authors (
    book_isbn VARCHAR(13) NOT NULL REFERENCES books (isbn) ON DELETE CASCADE,
    author_id BIGINT      NOT NULL REFERENCES authors (id) ON DELETE CASCADE,
    PRIMARY KEY (book_isbn, author_id)
);

-- Читатели
CREATE TABLE readers (
    card_number VARCHAR(7)  PRIMARY KEY,
    full_name   VARCHAR(255) NOT NULL,
    category    VARCHAR(20)  NOT NULL CHECK (category IN ('STUDENT', 'TEACHER', 'GUEST'))
);

-- Выдачи книг
CREATE TABLE loans (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_isbn   VARCHAR(13) NOT NULL REFERENCES books (isbn),
    card_number VARCHAR(7)  NOT NULL REFERENCES readers (card_number),
    issued_on   DATE        NOT NULL,
    due_on      DATE        NOT NULL,
    returned_on DATE,
    CHECK (due_on >= issued_on),
    CHECK (returned_on IS NULL OR returned_on >= issued_on)
);

-- Индексы для частых фильтров по выдачам
CREATE INDEX idx_loans_card_number ON loans (card_number);
CREATE INDEX idx_loans_book_isbn   ON loans (book_isbn);

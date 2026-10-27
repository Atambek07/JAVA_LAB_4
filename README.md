# 🏛️ Library — Лабораторная работа № 4: PostgreSQL, SQL, JDBC и Spring Data JPA

Проект **Library** — лабораторная работа № 4 по курсу Java (миграция приложения с
in-memory хранилища на базу данных **PostgreSQL**).

> **Предыдущая лабораторная (№ 3):** REST API на Spring Boot. Контракты REST полностью
> сохранены — заменилась только внутренняя реализация хранения данных.

## 📌 Краткое описание

Веб-приложение «Библиотека» предоставляет REST API для управления каталогом книг,
читателями и выдачами. Хранение данных — **PostgreSQL 17** (Docker Compose),
схема управляется **Flyway**, доступ — **Spring Data JPA** (+ отчёты на **JdbcClient**).

## ⚙️ Требования к окружению

| Компонент | Версия |
|---|---|
| Java | **21** |
| Maven | 3.8.7+ |
| Spring Boot | **4.1.2** |
| PostgreSQL | **17** (Docker) |
| Docker + Docker Compose | любой свежий |

## 🚀 Быстрый старт

```bash
# 1) Запустить базу данных (PostgreSQL 17) в фоне
export DB_PASSWORD=secret
docker compose up -d

# 2) Собрать и прогнать тесты (нужна запущенная БД)
export JAVA_HOME=/home/atambek/.jdks/ms-21.0.12.1   # при необходимости
mvn clean test

# 3) Запустить приложение
mvn spring-boot:run
```

> **Если порт 5432 уже занят** другим PostgreSQL (например, локальной установкой),
> задайте другой порт контейнера и приложения:
>
> ```bash
> export DB_PORT=5433
> docker compose up -d
> ```
>
> Порт параметризован: по умолчанию 5432 (как в задании), переопределяется `DB_PORT`.
> Пароль БД хранится только в переменной окружения `DB_PASSWORD`, не в файлах.

После запуска:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs
- REST API: http://localhost:8080/api/...

## 🗃️ База данных и миграции

```bash
docker exec -it library-postgres psql -U library -d library
# история миграций
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

Миграции: `src/main/resources/db/migration/V1__init.sql` (таблицы books, authors,
book_authors, readers, loans + индексы) и `V2__seed.sql` (4 книги, 5 авторов, 3 читателя).
Схема применяется автоматически при старте приложения; Hibernate использует
`ddl-auto=validate`.

## ⚒️ Команды Maven

```bash
mvn package -DskipTests          # сборка без тестов
mvn test                         # тесты (требуют запущенной БД)
mvn clean test -Dmaven.wagon.http.retryHandler.count=10   # чистый прогон с ретраями сети
mvn spring-boot:run              # запуск на :8080
```

## 🔌 Основные эндпоинты (контракты лаборатории № 3, без изменений)

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/books` | список книг (с `?q=...` — поиск по названию) |
| `POST/DELETE` | `/api/books`, `/api/books/{isbn}` | добавление / удаление |
| `GET/POST` | `/api/readers` | список / регистрация читателя |
| `GET` | `/api/readers/{card}/loans` | активные выдачи читателя |
| `POST` | `/api/loans` | выдача книги |
| `POST` | `/api/loans/{id}/return` | возврат книги |
| `POST` | `/api/loans/batch` | **пакетная выдача** (одна транзакция, полный откат при сбое) |
| `GET` | `/api/loans` | выдачи (`?overdue=true`) |
| `GET` | `/api/reports/top?n=5` | топ-N популярных книг |
| `GET` | `/api/reports/by-category` | выдачи по категориям |
| `GET` | `/api/reports/authors` | все авторы |

## 🗂️ Документация

| Документ | Путь |
|---|---|
| 📘 **Полный гайд (MD)** | [`docs/LAB4_COMPLETE_GUIDE.md`](docs/LAB4_COMPLETE_GUIDE.md) |
| 📑 **Полный гайд (PDF)** | [`docs/LAB4_COMPLETE_GUIDE.pdf`](docs/LAB4_COMPLETE_GUIDE.pdf) |
| 📙 **Handoff для лабораторной № 5** | [`docs/LAB5_AGENT_HANDOFF.md`](docs/LAB5_AGENT_HANDOFF.md) |
| 📗 **Гайд лабораторной № 3** | [`docs/LAB3_COMPLETE_GUIDE.md`](docs/LAB3_COMPLETE_GUIDE.md) |
| 🔌 **Примеры API** | [`docs/API_EXAMPLES.md`](docs/API_EXAMPLES.md) |

# 🏛️ Library — Лабораторная работа № 5: поиск, пагинация и тестирование

Проект **Library** — лабораторная работа № 5 по курсу Java. Приложение «Библиотека»
на **Spring Boot 4.1.2** с **PostgreSQL 17**, **Flyway**, **Spring Data JPA**,
пагинацией, динамическими спецификациями и оптимистической блокировкой.

> **Предыдущая лабораторная (№ 4):** PostgreSQL, SQL, JDBC и Spring Data JPA.
> **Лабораторная № 3:** REST API на Spring Boot.
> Списковые эндпоинты изменены требованием лабы № 5: теперь они возвращают
> `PageResponse<T>`, а не JSON-массив.

## 📌 Краткое описание

REST API для управления каталогом книг, читателями, выдачами и пользователями.

Реализовано в лабораторной № 5:

- пагинация и сортировка (`page`, `size`, `sort`) с ограничением полей;
- динамическая фильтрация через `Specification` (`/api/books/search`);
- поиск выдач по читателю, статусу и диапазону дат;
- оптимистическая блокировка (`@Version`, `PUT /api/books/{isbn}`);
- слоистые тесты: unit, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`;
- Testcontainers PostgreSQL 17 для интеграционных тестов;
- покрытие JaCoCo с порогом 60%.

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
cd library

# 1) База данных для запуска приложения
export DB_PASSWORD=secret
# На этой машине порт 5432 занят локальным PostgreSQL 16:
export DB_PORT=5433
docker compose up -d

# 2) Полная проверка: тесты + JaCoCo report + check
export JAVA_HOME=/home/atambek/.jdks/ms-21.0.12.1
mvn clean verify

# 3) Запуск приложения
mvn spring-boot:run
```

> **Тесты не требуют `docker compose`:** Testcontainers сам поднимает
> PostgreSQL 17 в отдельном контейнере со случайным портом.
>
> **Если порт 5432 свободен**, `DB_PORT` можно не задавать: по умолчанию
> используется 5432. Пароль БД хранится только в переменной окружения
> `DB_PASSWORD`, не в файлах.

После запуска:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI: <http://localhost:8080/v3/api-docs>
- REST API: <http://localhost:8080/api/...>

## 🗃️ База данных и миграции

```bash
docker exec -it library-postgres psql -U library -d library
# история миграций
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

Миграции:

| Файл | Назначение |
|---|---|
| `V1__init.sql` | таблицы `books`, `authors`, `book_authors`, `readers`, `loans` |
| `V2__seed.sql` | 4 книги, 5 авторов, 3 читателя |
| `V3__add.sql` | таблица `users` (доп. фича) |
| `V4__book_version.sql` | колонка `books.version` для оптимистической блокировки |

Схема применяется автоматически при старте; Hibernate использует
`ddl-auto=validate`. Если база создавалась старой (сломанной) версией
`V3__add.sql`, пересоздайте том:

```bash
docker compose down -v
docker compose up -d
```

## ⚒️ Команды Maven

```bash
mvn clean test                    # тесты (Testcontainers, Docker обязателен)
mvn clean verify                  # тесты + JaCoCo report + check (порог 60%)
mvn package -DskipTests           # сборка без тестов
mvn spring-boot:run               # запуск на :8080
```

В проекте нет Maven Wrapper (`mvnw`), используется системный `mvn`.

## 🔌 Основные эндпоинты

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/books?page&size&sort&q` | список книг с пагинацией; `sort` только `title`/`year` |
| `GET` | `/api/books/search?title&author&yearFrom&yearTo&available` | динамический поиск книг |
| `GET/POST/PUT/DELETE` | `/api/books`, `/api/books/{isbn}` | CRUD книги; `PUT` проверяет `version` |
| `GET/POST` | `/api/readers` | список / регистрация читателя |
| `GET` | `/api/readers/{card}/loans` | активные выдачи читателя |
| `GET` | `/api/loans?cardNumber&status&from&to&page&size` | выдачи с фильтрами и пагинацией |
| `POST` | `/api/loans` | выдача книги |
| `POST` | `/api/loans/{id}/return` | возврат книги |
| `POST` | `/api/loans/batch` | пакетная выдача (одна транзакция, полный откат при сбое) |
| `GET` | `/api/reports/top?n=5` | топ-N популярных книг |
| `GET` | `/api/reports/by-category` | выдачи по категориям |
| `GET` | `/api/reports/authors` | все авторы |
| `GET/POST/PUT/DELETE` | `/api/users`, `/api/users/{id}` | CRUD пользователей (доп. фича) |

Ошибки — `application/problem+json` (RFC 9457). Для ошибок валидации
дополнительно приходит свойство `errors` со списком `{field, message}`.

## ✅ Тесты и покрытие

| Класс | Тип | Тестов |
|---|---|---:|
| `LibraryServiceTest` | unit + Mockito | 14 |
| `BookControllerTest` | `@WebMvcTest` | 10 |
| `RepositoryTest` | `@DataJpaTest` + Testcontainers | 20 |
| `LibraryApiIntegrationTest` | `@SpringBootTest` + Testcontainers | 24 |
| `EntitiesValidationTest` | unit | 20 |
| `LibraryLab2Test` | unit | 8 |
| `LibraryTest` | unit | 5 |
| **Итого** | | **101** |

После `mvn clean verify`:

- `INSTRUCTION` — **76.89%**;
- `BRANCH` — **68.45%**;
- `LINE` — **78.48%**;
- порог JaCoCo (60%) пройден, `BUILD SUCCESS`.

Отчёт: `target/site/jacoco/index.html`.

## 🗂️ Документация

Документы организованы по лабораторным работам в папках `docs/lab3/`, `docs/lab4/`, `docs/lab5/`.

| Лабораторная | Документ | Путь |
|---|---|---|
| **№ 5** | 📘 Полный гайд лабораторной № 5 (MD) | [`docs/lab5/LAB5_COMPLETE_GUIDE.md`](docs/lab5/LAB5_COMPLETE_GUIDE.md) |
| **№ 5** | 📙 Handoff для лабораторной № 5 (из лабы № 4) | [`docs/lab4/LAB5_AGENT_HANDOFF.md`](docs/lab4/LAB5_AGENT_HANDOFF.md) |
| **№ 5** | 10 задач с кодом и объяснением | [`docs/lab5/10_ЗАДАНИЙ_ЛАБА5_С_КОДОМ_И_ОБЪЯСНЕНИЕМ.md`](docs/lab5/10_ЗАДАНИЙ_ЛАБА5_С_КОДОМ_И_ОБЪЯСНЕНИЕМ.md) |
| **№ 5** | Гайд: куда писать код (лаба 5) | [`docs/lab5/КАК_ПИСАТЬ_КОД_ЛАБА5_SPRING_BOOT.md`](docs/lab5/КАК_ПИСАТЬ_КОД_ЛАБА5_SPRING_BOOT.md) |
| **№ 5** | Все задания лабы 5 — куда писать код | [`docs/lab5/ВСЕ_ЗАДАНИЯ_ЛАБА5_КУДА_ПИСАТЬ_КОД.md`](docs/lab5/ВСЕ_ЗАДАНИЯ_ЛАБА5_КУДА_ПИСАТЬ_КОД.md) |
| **№ 5** | Общий гайд: куда писать код | [`docs/lab5/ГАЙД_КУДА_ПИСАТЬ_КОД.md`](docs/lab5/ГАЙД_КУДА_ПИСАТЬ_КОД.md) |
| **№ 4** | 📗 Полный гайд лабораторной № 4 | [`docs/lab4/LAB4_COMPLETE_GUIDE.md`](docs/lab4/LAB4_COMPLETE_GUIDE.md) |
| **№ 4** | 📗 Полный гайд лабораторной № 4 (PDF) | [`docs/lab4/LAB4_COMPLETE_GUIDE.pdf`](docs/lab4/LAB4_COMPLETE_GUIDE.pdf) |
| **№ 4** | Handoff из лабы № 3 → лаба № 4 | [`docs/lab3/LAB4_AGENT_HANDOFF.md`](docs/lab3/LAB4_AGENT_HANDOFF.md) |
| **№ 3** | 📕 Полный гайд лабораторной № 3 | [`docs/lab3/LAB3_COMPLETE_GUIDE.md`](docs/lab3/LAB3_COMPLETE_GUIDE.md) |
| **№ 3** | 📕 Полный гайд лабораторной № 3 (PDF) | [`docs/lab3/LAB3_COMPLETE_GUIDE.pdf`](docs/lab3/LAB3_COMPLETE_GUIDE.pdf) |
| **№ 3** | 🔌 Примеры API (лаба 3) | [`docs/lab3/API_EXAMPLES.md`](docs/lab3/API_EXAMPLES.md) |

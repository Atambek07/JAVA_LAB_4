# LAB5_AGENT_HANDOFF — Документ передачи для лабораторной работы № 5

> **Назначение.** Передаточный документ от ИИ-агента, выполнившего лабораторную работу № 4,
> агенту, который будет выполнять лабораторную работу № 5. Содержит **подтверждённые факты**
> о текущем состоянии проекта (задание № 5 пока неизвестно). Предположения отдельно помечены.

---

## 1. Текущее состояние проекта

Проект **Library** — полностью реализованная лабораторная работа № 4
(«PostgreSQL, SQL, JDBC и Spring Data JPA») на основе REST API лабораторной работы № 3.

- **Расположение:** `/home/atambek/LibraryProject_Lab4/library/`
- **Статус:** все 7 + дополнительное задания реализованы; **`mvn clean test` — 37/37 зелёных**
  (24 интеграционных + 8 юнит лабы № 2 + 5 юнит лабы № 2); PostgreSQL 17 поднят в Docker;
  миграции Flyway (2) применяются автоматически; N+1 устранён `@EntityGraph`;
  пакетная выдача с откатом проверена вживую.

## 2. Технологический стек и версии

| Компонент | Версия | Примечание |
|---|---|---|
| Java | 21 | JDK: `/home/atambek/.jdks/ms-21.0.12.1` |
| Maven | 3.8.7 | запускать с `JAVA_HOME` на JDK 21 |
| Spring Boot | **4.1.2** | родительский POM |
| Hibernate ORM | **7.4.13** | приходит с Boot 4.1 |
| PostgreSQL | **17** | образ `postgres:17` в Docker Compose (текущий 17.11) |
| Flyway | **11** | `spring-boot-starter-flyway` + `flyway-database-postgresql` |
| Spring JDBC | из Boot | `JdbcClient` для отчётов |
| springdoc-openapi | 3.1.1 | Swagger UI |
| Тесты | JUnit 5 + MockMvc | `spring-boot-starter-webmvc-test` |
| Group/Artifact | `kg.course` / `library` | |

> **Важно для Boot 4.1:** `Spring.main.allow-bean-definition-overriding=false`;
> пакет webmvc-тестов — `org.springframework.boot.webmvc.test.autoconfigure`;
> `ResponseEntityExceptionHandler` использует `HttpStatusCode`. Hibernate 7 **запрещает**
> `AttributeConverter` на `@Id` — поэтому `Isbn` реализован как `@Embeddable` + `@EmbeddedId`.

## 3. Как поднять окружение (этот компьютер)

```bash
# На этом компьютере порт 5432 занят локальным PostgreSQL 16 → используем 5433.
export DB_PASSWORD=secret
export DB_PORT=5433
cd /home/atambek/LibraryProject_Lab4/library
docker compose up -d          # контейнер library-postgres (postgres:17)
export JAVA_HOME=/home/atambek/.jdks/ms-21.0.12.1
mvn clean test                # 37/37 зелёных (требует запущенной БД)
mvn spring-boot:run           # приложение на :8080
```

- По умолчанию порт 5432 (по заданию). Если 5432 свободен — `export DB_PORT` не нужен.
- Приложение мигрирует схему само (Flyway). Свежий старт: `docker compose down -v && docker compose up -d`.
- Swagger UI: http://localhost:8080/swagger-ui.html ; OpenAPI: /v3/api-docs.

## 4. Схема базы данных (PostgreSQL 17, БД `library`, схема `public`)

Миграции: `src/main/resources/db/migration/` → `V1__init.sql`, `V2__seed.sql`
(история — таблица `flyway_schema_history`, 2 записи `success=t`).

| Таблица | Столбцы | Ограничения |
|---|---|---|
| `books` | `isbn`, `title`, `year`, `copies` | PK `isbn varchar(13)`; `title NOT NULL`; `year>=1450`; `copies>=1` |
| `authors` | `id`, `name` | PK `id bigint generated always as identity`; `name` UNIQUE NOT NULL |
| `book_authors` | `book_isbn`, `author_id` | составной PK; FK на books/authors с CASCADE |
| `readers` | `card_number`, `full_name`, `category` | PK; `category IN ('STUDENT','TEACHER','GUEST')` |
| `loans` | `id`, `book_isbn`, `card_number`, `issued_on`, `due_on`, `returned_on` | PK identity; FK; `due_on>=issued_on`; `returned_on IS NULL OR >= issued_on` |

Индексы: `idx_loans_card_number`, `idx_loans_book_isbn`. Seed: 4 книги, 5 авторов,
3 читателя (R-00001 STUDENT, R-00002 GUEST, R-00003 TEACHER). ISBN в БД хранятся
нормализованными (без дефисов).

## 5. Сущности JPA (пакет `kg.course.library.entity`)

| Класс | PK | Связи | Примечание |
|---|---|---|---|
| `Book` | `@EmbeddedId Isbn isbn` | `@ManyToMany → authors` (join `book_authors`) | 
| `Author` | `@GeneratedValue(IDENTITY) Long id` | обратная сторона ManyToMany | `name` уникален |
| `Reader` | `String cardNumber` | — | `@Enumerated(EnumType.STRING) category` |
| `Loan` | `@GeneratedValue(IDENTITY) Long id` | `@ManyToOne(LAZY) book`, `@ManyToOne(LAZY) reader` | методы `isActive()`, `isOverdue(LocalDate)`, `markReturned(LocalDate)` |

Общее: protected no-args ctor + открытый ctor с валидацией; `equals/hashCode` только по
PK. `Isbn` — `@Embeddable record` в `kg.course.library` (легаси-классы лабы 2/3 сохранены).

## 6. Репозитории (пакет `kg.course.library.repository`)

| Репозиторий | Ключевые методы |
|---|---|
| `BookRepository` | `findAllByOrderByTitleAsc`, `findByIsbn`, `findByTitleContainingIgnoreCaseOrderByTitleAsc` (все с `@EntityGraph("authors")`) |
| `AuthorRepository` | `findByName` |
| `ReaderRepository` | стандартные |
| `LoanRepository` | `findAllOrdered`, `findLoanById`, `findByReaderCardNumberAndReturnedOnIsNull`, `findByReturnedOnIsNullAndDueOnBeforeOrderByDueOnAsc` (все с `@EntityGraph({"book","reader"})`), счётчики `countBy*`, `existsBy*`, `deleteByBookIsbn` |
| `ReportRepository` | `JdbcClient`: `topBooks(n)`, `loansByCategory()`, `allAuthors()`, `searchBooksByTitle(fragment)` (безопасный, параметризованный) |

## 7. Сервисный слой

`kg.course.library.LibraryService` — `@Service`, полностью заменяет class `Library` лабы № 3.
Чтение — `@Transactional(readOnly=true)`, запись — `@Transactional`.

Пять проверок выдачи (порядок из лабы № 2): читатель есть → книга есть → нет просрочек →
лимит категории (`ReaderCategory.maxBooks()`) → есть свободный экземпляр (`copies - активные`).

Методы: `findAllBooks/searchByTitle/findBook/getBook/totalCopies/availableCopies/addBook/removeBook`,
`findAllReaders/findReader/getReader/registerReader`, `findAllLoans/getLoan/overdueLoans/activeLoans`,
`issueBook`, `issueBooksBatch` (одна транзакция, полный откат при сбое), `returnBook`.

## 8. НЕприкасаемые контракты REST API (лаба № 3): не менять пути/JSON/коды

| Метод | Путь | Успех | Ошибки |
|---|---|---|---|
| GET | `/api/books` (`?q=`) | 200 | — |
| GET | `/api/books/{isbn}` | 200 | 404 |
| POST | `/api/books` | 201 + Location | 400, 409 |
| DELETE | `/api/books/{isbn}` | 204 | 404, 409 |
| GET | `/api/readers` | 200 | — |
| GET | `/api/readers/{cardNumber}` | 200 | 404 |
| POST | `/api/readers` | 201 + Location | 400, 409 |
| GET | `/api/readers/{cardNumber}/loans` | 200 | 404 |
| GET | `/api/loans` (`?overdue=true`) | 200 | — |
| GET | `/api/loans/{id}` | 200 | 404 |
| POST | `/api/loans` | 201 + Location | 400, 404, 409 |
| POST | `/api/loans/{id}/return` | 200 | 404, 409 |
| POST | `/api/loans/batch` (**новое в лабе № 4**) | 201 + `List<LoanResponse>` | 400, 404, 409 |
| GET | `/api/reports/top?n=5` | 200 | 400 (n<1) |
| GET | `/api/reports/by-category` | 200 | — |
| GET | `/api/reports/authors` | 200 | — |

- Request DTO: `CreateBookRequest`, `CreateReaderRequest`, `IssueRequest`, `BatchIssueRequest`.
- Response DTO: `BookResponse`, `ReaderResponse`, `LoanResponse` (records, в `web`).
- Ошибки: ProblemDetail RFC 9457 (`application/problem+json`), поля
  `type,title,status,detail,instance` (+`errors[]` при валидации). 404 `NotFoundException`,
  409 `BookUnavailable/LimitExceeded/Overdue/IllegalState/DataIntegrityViolation`, 400 —
  валидация/`IllegalArgumentException`, 500 — безопасный.
- ISBN в ответах нормализованный (без дефисов).
- Категория в JSON — имя enum: `"STUDENT"`, `"TEACHER"`, `"GUEST"`.

## 9. Транзакции и N+1

- `@Transactional` — граница на методе сервиса; `RuntimeException` → полный откат.
- `open-in-view=false` — ассоциации подгружаются внутри транзакции через `@EntityGraph`.
- N+1 устранён для `GET /api/loans`: один `SELECT ... JOIN books JOIN readers` (подтверждено
  в журнале при `spring.jpa.show-sql=true`).

## 10. Тесты

`mvn clean test` (нужна запущенная БД) — 37 зелёных:
- `LibraryApiIntegrationTest` (24) — MockMvc поверх PostgreSQL; перед каждым тестом
  таблицы чистятся через `JdbcTemplate` (loans→book_authors→books→readers); фиксированный
  `Clock` (2026-03-20); добавлены тесты batch-отката и защиты от SQL-инъекции.
- `LibraryTest` (5), `LibraryLab2Test` (8) — юнит лабы № 2 (используют легаси-классы).
- Тест-инфраструктура: `TestClockConfig` (fixed clock @Primary), `TestBoomController` (500).

Осторожно: не удаляйте легаси-классы `kg.course.library.*` (Library, LibraryReports, records
Book/Reader/Loan, BookCsvImporter, ReportWriter, Isbn, ReaderCategory...) — на них стоят
юнит-тесты лабы № 2/3.

## 11. Известные особенности

1. Порт: на этой машине 5432 занят локальным PostgreSQL 16; контейнер слушает 5433
   (`DB_PORT=5433`). В `compose.yaml` и `application.properties` порт параметризован
   (`${DB_PORT:-5432}`) — по умолчанию 5432, как в задании.
2. `spring.jpa.show-sql=true` и подробное логирование SQL включены (доп. задание) — при
   желании отключить.
3. `removeBook` удаляет и прошлые (возвращённые) выдачи книги, чтобы не нарушать FK
   `loans.book_isbn` (активные выдачи → 409). Это задокументированное решение.
4. Сущности и легаси-записи имеют одинаковые имена в разных пакетах (`entity.*` vs корень) —
   следите за импортами.

## 12. Правила для агента лабы № 5 (жёсткие)

1. **Сначала прочитать** этот файл, `docs/LAB4_COMPLETE_GUIDE.md`, `README.md`,
   `docs/API_EXAMPLES.md` (контракты) и дождаться текста задания № 5 — не выдумывать задание.
2. **Не менять** пути, JSON, HTTP-коды и формат ошибок существующих эндпоинтов (раздел 8).
3. Новые функции — в сервисном слое; новые эндпоинты — новый метод контроллера + DTO.
4. Новые ошибки — класс исключения + маппинг в `ApiExceptionHandler`.
5. Схему менять **только** новыми Flyway-миграциями (`V3__...sql`) — изменять/удалять
   применённые миграции нельзя (flyway checksum).
6. `mvn clean test` в конце должен быть 100% зелёным (`export DB_PASSWORD=secret DB_PORT=5433`).
7. Обновлять README и docs при изменении.

## 13. Документация

| Документ | Путь |
|---|---|
| Полный гайд лабы № 4 (MD) | `docs/LAB4_COMPLETE_GUIDE.md` |
| Полный гайд лабы № 4 (PDF) | `docs/LAB4_COMPLETE_GUIDE.pdf` |
| Handoff этой лабы | `docs/LAB5_AGENT_HANDOFF.md` |
| Гайд лабы № 3 | `docs/LAB3_COMPLETE_GUIDE.md` |
| Примеры API | `docs/API_EXAMPLES.md` |
| README | `README.md` |

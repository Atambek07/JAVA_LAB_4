# 📘 Гайд: Лабораторная работа № 5 (Spring Boot) — куда писать код

> **Проект:** `..` (Spring Boot 4.1.2, Java 21, PostgreSQL 17, Flyway, Testcontainers)
> **Путь к коду:** `library/src/main/java/kg/course/library/...`
> **Путь к тестам:** `library/src/test/java/kg/course/library/...`

Этот файл — только справочник. Он объясняет:

1. **Куда писать код** (в какую папку и пакет класть новый файл для лабы № 5).
2. **Как писать код** (какие аннотации, слои и соглашения используются в этом проекте).
3. **Какие задания может дать преподаватель** (несколько вариантов, по главам документации).

**Правило:** существующий код не меняем — только добавляем новые файлы или новые методы в уже существующие сервисы/контроллеры/репозитории.

---

## 1. Структура пакетов (по документации лабы № 5)

```
library/src/main/java/kg/course/library/
├── LibraryApplication.java        # точка входа — НЕ ТРОГАТЬ
├── LibraryService.java            # сервисный слой, бизнес-правила, @Transactional
├── UserService.java               # CRUD пользователей (доп. фича)
├── LibraryReports.java            # отчёты (JdbcClient)
├── LibraryUtils.java, BookCsvImporter.java, ReportWriter.java
├── LibraryException.java, NotFoundException.java, BookUnavailableException.java,
│   LimitExceededException.java, OverdueException.java
├── Book.java, Reader.java, Loan.java, Isbn.java, ReaderCard.java, ReaderCategory.java,
│   Item.java, Describable.java, Magazine.java, PaperBook.java, BookStat.java,
│   ImportedBook.java, ImportResult.java
│
├── entity/                        # JPA-сущности (таблицы БД)
│   ├── Author.java, Book.java, Loan.java, Reader.java, User.java
│
├── repository/                    # Spring Data JPA репозитории + спецификации
│   ├── AuthorRepository.java
│   ├── BookRepository.java        # extends JpaRepository<Book, Isbn>, JpaSpecificationExecutor<Book>
│   ├── BookSpecifications.java    # статические методы hasTitle, hasAuthor, yearBetween, hasAvailableCopies, search
│   ├── LoanRepository.java        # extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan>
│   ├── LoanSpecifications.java    # hasCardNumber, hasStatus, issuedFrom, issuedTo, search
│   ├── ReaderRepository.java
│   ├── ReportRepository.java      # JdbcClient (отчёты SQL)
│   └── UserRepository.java
│
├── web/                           # REST-контроллеры, DTO, обработчик ошибок
│   ├── LibraryConfig.java         # конфигурация (в пакете config/)
│   ├── BookController.java        # GET/POST/PUT/DELETE /api/books, GET /api/books/search
│   ├── LoanController.java        # GET/POST /api/loans, POST /api/loans/{id}/return, POST /api/loans/batch
│   ├── ReaderController.java      # GET/POST /api/readers, GET /api/readers/{card}/loans
│   ├── ReportController.java      # GET /api/reports/...
│   ├── UsersController.java       # GET/POST/PUT/DELETE /api/users
│   ├── ApiExceptionHandler.java   # @ControllerAdvice — 400/404/409/500 → ProblemDetail (RFC 9457)
│   ├── PageResponse.java          # record: content, page, size, totalElements, totalPages
│   ├── CreateBookRequest.java, UpdateBookRequest.java, BookResponse.java
│   ├── CreateReaderRequest.java, ReaderResponse.java
│   ├── IssueRequest.java, BatchIssueRequest.java, LoanResponse.java
│   ├── CreateUserRequest.java, UpdateUserRequest.java, UserResponse.java
│   └── InvalidSortException.java
│
└── config/
    └── LibraryConfig.java
```

**Ресурсы (БД, настройки):**

```
library/src/main/resources/
├── application.properties         # настройки приложения, БД, пагинация, логирование SQL
└── db/migration/
    ├── V1__init.sql               # таблицы books, authors, book_authors, readers, loans
    ├── V2__seed.sql               # 4 книги, 5 авторов, 3 читателя
    ├── V3__add.sql                # таблица users (доп. фича)
    └── V4__book_version.sql       # колонка books.version (@Version)
```

**Тесты:**

```
library/src/test/java/kg/course/library/
├── LibraryServiceTest.java        # unit + Mockito (14 тестов)
├── LibraryTest.java, LibraryLab2Test.java
├── EntitiesValidationTest.java    # unit (20 тестов)
├── web/BookControllerTest.java    # @WebMvcTest (10 тестов)
├── repository/RepositoryTest.java # @DataJpaTest + Testcontainers (20 тестов)
└── LibraryApiIntegrationTest.java # @SpringBootTest + Testcontainers (24 тестов)
```

---

## 2. Куда писать код — таблица по назначению (лаба № 5)

| Что нужно добавить | Папка (от `../src/main/java`) | Пример имени | Аннотации / правила |
|--------------------|-----------------------------------|--------------|---------------------|
| Новая JPA-сущность (таблица) | `kg/course/library/entity/` | `Genre.java` | `@Entity`, `@Table`, `@Id` |
| Миграция БД под новую таблицу | `kg/course/library/../resources/db/migration/` | `V5__add_genre.sql` | Flyway: `V<номер>__<описание>.sql`, НЕ менять старые миграции (контрольная сумма Flyway) |
| Репозиторий сущности | `kg/course/library/repository/` | `GenreRepository.java` | `extends JpaRepository<Genre, Long>` (+ `JpaSpecificationExecutor` если нужен поиск) |
| Спецификации (динамический фильтр) | `kg/course/library/repository/` | `GenreSpecifications.java` | статические методы `Specification<Genre>` по образцу `BookSpecifications` |
| Бизнес-логика | `kg/course/library/` | `LibraryService.java` (добавить метод) или `GenreService.java` | `@Service`, `@Transactional` на метод записи |
| REST-контроллер | `kg/course/library/web/` | `GenreController.java` | `@RestController`, `@RequestMapping("/api/...")` |
| DTO запроса (тело POST/PUT) | `kg/course/library/web/` | `CreateGenreRequest.java` | `@Valid`, `@NotBlank`, `@Min`, `@Max` |
| DTO ответа | `kg/course/library/web/` | `GenreResponse.java` | `record`, не возвращать сущность `entity` напрямую |
| Кастомное исключение | `kg/course/library/` | `GenreNotFoundException.java` | extends `RuntimeException` или `LibraryException` |
| Обработчик исключения | `kg/course/library/web/ApiExceptionHandler.java` | — | `@ExceptionHandler(...)` → `ResponseEntity` с `application/problem+json` |
| Настройки приложения | `../src/main/resources/application.properties` | — | только дописать свои ключи, не удалять существующие |
| Тест контроллера | `../src/test/java/kg/course/library/web` | `GenreControllerTest.java` | `@WebMvcTest`, `@MockitoBean` (Boot 4.1: пакет `org.springframework.test.context.bean.override.mockito.MockitoBean`) |
| Тест репозитория | `../src/test/java/kg/course/library/repository` | `GenreRepositoryTest.java` | `@DataJpaTest` + Testcontainers PostgreSQL 17 |
| Тест сервиса | `../src/test/java/kg/course/library` | `GenreServiceTest.java` | JUnit 5 + Mockito (`@Mock`, `@InjectMocks`) |
| Интеграционный тест | `../src/test/java/kg/course/library` | `LibraryApiIntegrationTest.java` (добавить метод) | `@SpringBootTest` + Testcontainers, MockMvc |

---

## 3. Как писать код — соглашения проекта (из документации лабы № 5)

### 3.1. Общие правила
- **Пакет:** `kg.course.library` и подпакеты (`entity`, `repository`, `web`, `config`).
- **Классы:** `PascalCase` (`BookRepository`, `CreateBookRequest`).
- **Метоля/поля:** `camelCase` (`findAllBooks`, `availableCopies`).
- **DTO:** отдельные `record` или классы в `web/` (`*Request`, `*Response`). **Не возвращайте JPA-сущности клиенту напрямую.**
- **Валидация:** `@Valid` на `@RequestBody`, `@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Positive`.
- **Ошибки:** выбрасывайте исключение в сервисе → `ApiExceptionHandler` (`@ControllerAdvice`) превращает в `application/problem+json` (RFC 9457) со статусом 400/404/409/500.
- **Сущность БД:** `@Entity` + `@Table` + `@Id` (+ `@Version` для оптимистической блокировки).
- **Репозиторий:** интерфейс, `extends JpaRepository<Сущность, ТипId>`. Поисковые методы — по имени (`findBy...`) или `@Query`.

### 3.2. Контроллер (web) — только HTTP
Контроллер принимает запрос, валидирует параметры, вызывает сервис, возвращает DTO.

Пример (из документации, главы 3–5):

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final LibraryService library;

    @GetMapping
    public PageResponse<BookResponse> findAll(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10) Pageable pageable) {
        validatePageable(pageable);
        Page<Book> page = library.findBooks(q, pageable);
        return PageResponse.from(page.map(b ->
            BookResponse.from(b, library.availableCopies(b.getIsbn()))));
    }

    @GetMapping("/search")
    public PageResponse<BookResponse> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Boolean available,
            @PageableDefault(size = 10) Pageable pageable) {
        validatePageable(pageable);
        Page<Book> page = library.searchBooks(
            BookSpecifications.search(title, author, yearFrom, yearTo, available),
            pageable);
        return PageResponse.from(page.map(b ->
            BookResponse.from(b, library.availableCopies(b.getIsbn()))));
    }

    @PutMapping("/{isbn}")
    public BookResponse update(@PathVariable String isbn,
                               @Valid @RequestBody UpdateBookRequest request) {
        Book book = library.updateBook(
            new Isbn(isbn), request.title(), request.year(), request.copies(), request.version());
        return BookResponse.from(book, library.availableCopies(book.getIsbn()));
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("Размер страницы не может быть больше 100");
        }
        Set<String> allowed = Set.of("title", "year");
        var bad = pageable.getSort().stream()
            .map(Order::getProperty)
            .filter(p -> !allowed.contains(p))
            .toList();
        if (!bad.isEmpty()) {
            throw new InvalidSortException(bad, allowed);
        }
    }
}
```

### 3.3. Сервис — бизнес-логика + `@Transactional`

```java
@Service
public class LibraryService {

    @Transactional(readOnly = true)
    public Page<Book> findBooks(String q, Pageable pageable) { ... }

    @Transactional
    public Book updateBook(Isbn isbn, String title, int year, int copies, Integer version) {
        Book book = getBook(isbn);
        if (!Objects.equals(book.getVersion(), version)) {
            throw new ObjectOptimisticLockingFailureException(Book.class, isbn.value());
        }
        long active = loanRepository.countByBookIsbnAndReturnedOnIsNull(isbn);
        if (copies < active) {
            throw new BookUnavailableException("...");
        }
        book.setTitle(title);
        book.setYear(year);
        book.setCopies(copies);
        return bookRepository.save(book);
    }
}
```

### 3.4. Репозиторий + спецификации (динамический поиск)

```java
public interface BookRepository
        extends JpaRepository<Book, Isbn>, JpaSpecificationExecutor<Book> {

    @Override
    @EntityGraph(attributePaths = "authors")
    Page<Book> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "authors")
    Page<Book> findAll(Specification<Book> spec, Pageable pageable);
}
```

Спецификация (Criteria API):

```java
public static Specification<Book> hasTitle(String fragment) {
    return (root, query, cb) -> {
        if (fragment == null || fragment.isBlank()) return null;
        return cb.like(cb.lower(root.get("title")),
                "%" + fragment.trim().toLowerCase() + "%");
    };
}
```

Комбинирование:

```java
public static Specification<Book> search(String title, String author,
                                         Integer yearFrom, Integer yearTo, Boolean available) {
    Specification<Book> spec = Specification.where(hasTitle(title));
    spec = spec.and(hasAuthor(author));
    spec = spec.and(yearBetween(yearFrom, yearTo));
    if (Boolean.TRUE.equals(available)) {
        spec = spec.and(hasAvailableCopies());
    }
    return spec;
}
```

### 3.5. Миграция БД (Flyway) — только новые файлы

Найти: `../src/main/resources/db/migration`

Имя файла: `V5__<описание>.sql` (следующий номер после `V4`).

Пример:

```sql
-- V5__add_genre.sql
CREATE TABLE genres (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

ALTER TABLE books ADD COLUMN genre_id BIGINT REFERENCES genres(id);
```

> ⚠️ **Не меняйте и не удаляйте существующие миграции** (`V1`–`V4`). Flyway хранит контрольные суммы в таблице `flyway_schema_history`, и приложение перестанет стартовать при изменении уже применённой миграции.

---

## 4. Варианты заданий, которые может дать преподаватель (по документации лабы № 5)

Ниже — несколько типичных вариантов. Для каждого указано, **что и куда писать**. Существующий код не трогаем.

---

### Вариант 1. Пагинация и сортировка (Задание 1 из документации)
**Суть:** список книг/читателей/выдач должен принимать `page`, `size`, `sort` и возвращать `PageResponse<T>` вместо JSON-массива. Сортировка только по разрешённым полям.

**Куда писать:**
- `web/BookController.java` — метод `findAll` с `@PageableDefault`, валидация `page`/`size`/`sort`, возврат `PageResponse<BookResponse>`.
- `web/PageResponse.java` — уже есть (`record`), использовать как обёртку.
- `web/InvalidSortException.java` — исключение для недопустимого поля сортировки.
- `web/ApiExceptionHandler.java` — добавить `@ExceptionHandler(InvalidSortException.class)` → 400 `application/problem+json`.
- `repository/BookRepository.java` — `extends JpaSpecificationExecutor<Book>`, `@EntityGraph` на `findAll(Pageable)`.

**Пример запроса:**
```
GET /api/books?page=0&size=5&sort=title,asc
```

---

### Вариант 2. Динамическая фильтрация через Specifications (Задание 2)
**Суть:** эндпоинт `GET /api/books/search` с фильтрами `title`, `author`, `yearFrom`, `yearTo`, `available` + пагинация.

**Куда писать:**
- `repository/BookSpecifications.java` — методы `hasTitle`, `hasAuthor`, `yearBetween`, `hasAvailableCopies`, `search` (комбинирование через `Specification.where(...).and(...)`).
- `repository/BookRepository.java` — `findAll(Specification<Book>, Pageable)` с `@EntityGraph`.
- `web/BookController.java` — метод `search(...)` с `@RequestParam` и `@PageableDefault`.
- `web/BookController.java` — вызов `library.searchBooks(spec, pageable)`.
- `library/LibraryService.java` — метод `searchBooks(Specification<Book>, Pageable)`.

**Пример запроса:**
```
GET /api/books/search?title=java&author=Блох&yearFrom=2010&yearTo=2020&available=true&page=0&size=5
```

---

### Вариант 3. Поиск выдач с фильтрами (Задание 3)
**Суть:** `GET /api/loans` с фильтрами `cardNumber`, `status` (ACTIVE/RETURNED/OVERDUE), `from`, `to`, `overdue` + пагинация. Сортировка по умолчанию `issuedOn,desc`.

**Куда писать:**
- `repository/LoanSpecifications.java` — `hasCardNumber`, `hasStatus`, `issuedFrom`, `issuedTo`, `search`.
- `repository/LoanRepository.java` — `extends JpaSpecificationExecutor<Loan>`, `@EntityGraph({"book","reader"})`.
- `web/LoanController.java` — метод `findAll(...)` с параметрами фильтра, `validateStatus`, `LocalDate today = LocalDate.now(clock)`.
- `library/LibraryService.java` — метод `searchLoans(Specification<Loan>, Pageable)`.
- `web/ApiExceptionHandler.java` — обработка недопустимого `status` → 400.

**Пример запроса:**
```
GET /api/loans?cardNumber=R-00001&status=ACTIVE&from=2026-01-01&to=2026-03-31&page=0&size=10
```

---

### Вариант 4. Оптимистическая блокировка `@Version` (Задание 4)
**Суть:** `PUT /api/books/{isbn}` с проверкой версии. Конфликт → 409. Также нельзя уменьшить количество экземпляров ниже активных выдач.

**Куда писать:**
- `db/migration/V4__book_version.sql` — уже есть (`ALTER TABLE books ADD COLUMN version ...`).
- `entity/Book.java` — `@Version private Integer version;` + `implements Persistable<Isbn>` + `isNew()` (`version == null`).
- `web/BookResponse.java` — поле `version` в ответе.
- `web/UpdateBookRequest.java` — `title` (`@NotBlank`), `year` (`@Min(1450)`, `@Max(2200)`), `copies` (`@Min(1)`), `version` (`@NotNull`).
- `web/BookController.java` — `PUT /api/books/{isbn}` с `@Valid @RequestBody UpdateBookRequest`.
- `library/LibraryService.java` — метод `updateBook(...)`: проверка версии, проверка `copies >= activeLoans`, `bookRepository.save(book)`.
- `web/ApiExceptionHandler.java` — `@ExceptionHandler(ObjectOptimisticLockingFailureException.class)` → 409 Conflict.

**Пример запроса:**
```
PUT /api/books/9785446109149
Body: {"title":"...","year":2019,"copies":3,"version":0}
```

---

### Вариант 5. Модульные тесты сервиса (Задание 5)
**Суть:** покрыть `LibraryService` юнит-тестами с Mockito.

**Куда писать:**
- `../src/test/java/kg/course/library/LibraryServiceTest.java` — `@Mock` для репозиториев, `@InjectMocks` для `LibraryService`, `MockitoExtension`, фиксированный `Clock`.
- Проверки: успешная выдача, 5 правил выдачи (читатель есть, книга есть, нет просрочек, лимит категории, свободный экземпляр), `verify(save)`.

---

### Вариант 6. Тесты веб-слоя (Задание 6)
**Суть:** `@WebMvcTest` для контроллеров с моками сервисов.

**Куда писать:**
- `../src/test/java/kg/course/library/web/BookControllerTest.java` — `@WebMvcTest`, `@MockitoBean` (Boot 4.1: `org.springframework.test.context.bean.override.mockito.MockitoBean`), проверка HTTP-кодов, `Location`, `errors`, 404 `application/problem+json`, 400 при плохой сортировке.
- Аналогично можно добавить `LoanControllerTest`, `ReaderControllerTest` и т.д.

---

### Вариант 7. Интеграционные тесты с Testcontainers (Задание 7)
**Суть:** тесты на реальном PostgreSQL 17 через Testcontainers.

**Куда писать:**
- `../src/test/java/kg/course/library/repository/RepositoryTest.java` — `@DataJpaTest` + Testcontainers: миграции, репозитории, спецификации, CHECK/FK-ограничения, `@Version`.
- `../src/test/java/kg/course/library/LibraryApiIntegrationTest.java` — `@SpringBootTest` + Testcontainers, MockMvc, scénario «книга → читатель → выдача → 409 лимит → возврат → отчёт».

---

### Вариант 8. Покрытие JaCoCo (Задание 8)
**Суть:** порог покрытия 60% (`INSTRUCTION`, `BRANCH`). Если меньше — сборка падает.

**Куда писать:**
- `pom.xml` — уже настроен `jacoco-maven-plugin` (prepare-agent, report, check, порог 0.60).
- Если добавляете новый код — обязательно добавьте тесты, иначе JaCoCo check может не пройти.
- Проверить: `mvn clean verify` → отчёт в `target/site/jacoco/index.html`.

---

### Вариант 9 (доп.). CRUD пользователей (доп. фича из главы 7)
**Суть:** полноценный CRUD `/api/users` (таблица `users` из `V3__add.sql`).

**Куда писать:**
- `entity/User.java` — уже есть (`id`, `name`, `age`, `inn` 14-значное, уникально).
- `repository/UserRepository.java` — уже есть.
- `library/UserService.java` — уже есть (`register`, CRUD, проверка уникальности ИНН).
- `web/UsersController.java` — уже есть (`GET/POST/PUT/DELETE /api/users`).
- `web/CreateUserRequest.java`, `UpdateUserRequest.java`, `UserResponse.java` — уже есть.
- **Что можно добавить:** тесты `UserServiceTest` и `UsersControllerTest` (в документации сказано, что отдельных тестов для пользователей нет — это очевидный следующий шаг для повышения покрытия).

---

### Вариант 10 (доп.). Новая сущность + полный CRUD (например, Жанр/Издательство)
**Суть:** добавить новую таблицу и REST API для неё.

**Куда писать:**
1. `db/migration/V5__add_genre.sql` — таблица.
2. `entity/Genre.java` — `@Entity`, `@Table`, `@Id`.
3. `repository/GenreRepository.java` — `JpaRepository<Genre, Long>`.
4. `library/GenreService.java` (или метод в `LibraryService`) — `@Service`, `@Transactional`.
5. `web/GenreController.java` — `GET/POST/PUT/DELETE /api/genres`.
6. `web/CreateGenreRequest.java`, `GenreResponse.java` — DTO.
7. Тесты в `src/test/...` по соглашению выше.

---

## 5. Как проверить код (команды)

В папке `..`:

```bash
cd library

# Указать JDK 21
export JAVA_HOME=/home/atambek/.jdks/ms-21.0.12.1

# 1) База данных (Docker Compose, порт 5433 на этом компьютере)
export DB_PASSWORD=secret
export DB_PORT=5433
docker compose up -d

# 2) Запустить все тесты
mvn clean test

# 3) Тесты + отчёт покрытия JaCoCo (порог 60%, сборка упадёт если ниже)
mvn clean verify

# 4) Собрать без тестов
mvn package -DskipTests

# 5) Запустить приложение (http://localhost:8080)
mvn spring-boot:run
```

После запуска:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Отчёт покрытия: `target/site/jacoco/index.html`

---

## 6. Краткая шпаргалка «что → куда»

| Нужно сделать | Файл/папка | Что дописать |
|---------------|------------|--------------|
| Новая таблица в БД | `library/src/main/resources/db/migration/V5__...sql` | `CREATE TABLE ...` |
| Новая сущность JPA | `library/src/main/java/kg/course/library/entity/XXX.java` | `@Entity`, `@Table`, `@Id` |
| Репозиторий | `library/src/main/java/kg/course/library/repository/XXXRepository.java` | `extends JpaRepository<XXX, Long>` |
| Динамический фильтр | `library/src/main/java/kg/course/library/repository/XXXSpecifications.java` | статические `Specification<XXX>` методы |
| Бизнес-логика | `../src/main/java/kg/course/library/LibraryService.java` (или новый `*Service.java`) | `@Service`, `@Transactional`, новые методы |
| REST-эндпоинт | `library/src/main/java/kg/course/library/web/XXXController.java` | `@RestController`, `@RequestMapping("/api/...")` |
| DTO запроса | `library/src/main/java/kg/course/library/web/CreateXXXRequest.java` | `@Valid`, `@NotBlank`, и т.д. |
| DTO ответа | `library/src/main/java/kg/course/library/web/XXXResponse.java` | `record` |
| Исключение | `library/src/main/java/kg/course/library/XXXException.java` | extends `RuntimeException` |
| Обработчик исключения | `../src/main/java/kg/course/library/web/ApiExceptionHandler.java` | `@ExceptionHandler(...)` → 400/404/409 |
| Настройки | `../src/main/resources/application.properties` | дописать свои ключи |
| Тест контроллера | `library/src/test/java/kg/course/library/web/XXXControllerTest.java` | `@WebMvcTest`, `@MockitoBean` |
| Тест репозитория | `library/src/test/java/kg/course/library/repository/XXXRepositoryTest.java` | `@DataJpaTest` + Testcontainers |
| Тест сервиса | `library/src/test/java/kg/course/library/XXXServiceTest.java` | JUnit 5 + Mockito |

**Главное правило для этой работы:** существующий код не меняем — только добавляем новые файлы и новые методы в уже существующие классы.

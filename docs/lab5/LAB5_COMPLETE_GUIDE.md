# 📚 Лабораторная работа № 5 — Поиск, пагинация и тестирование Spring Boot приложений

**Проект:** `library` • **Предыдущая лаба:** [`LAB4_COMPLETE_GUIDE.md`](LAB4_COMPLETE_GUIDE.md) • **README:** [`../README.md`](../README.md)

> **Дата реализации:** 07.11.2026 • **Java 21** • **Spring Boot 4.1.2** • **Hibernate 7.4.13** •
> **PostgreSQL 17** • **Flyway 11.10.1** • **Testcontainers 2.0.5** • **JaCoCo 0.8.12**

---

## Содержание

1. [Общая информация](#глава-1-общая-информация)
2. [Архитектура и ключевые решения](#глава-2-архитектура-и-ключевые-решения)
3. [Задание 1. Пагинация и сортировка](#глава-3-задание-1-пагинация-и-сортировка)
4. [Задание 2. Фильтрация через Specifications](#глава-4-задание-2-фильтрация-через-specifications)
5. [Задание 3. Поиск выдач](#глава-5-задание-3-поиск-выдач)
6. [Задание 4. Оптимистическая блокировка](#глава-6-задание-4-оптимистическая-блокировка)
7. [Дополнительная фича. Пользователи (users)](#глава-7-дополнительная-фича-пользователи-users)
8. [Стратегия тестирования](#глава-8-стратегия-тестирования)
9. [Задание 5. Модульные тесты сервиса](#глава-9-задание-5-модульные-тесты-сервиса)
10. [Задание 6. Тесты веб-слоя](#глава-10-задание-6-тесты-веб-слоя)
11. [Задание 7. Интеграционные тесты с Testcontainers](#глава-11-задание-7-интеграционные-тесты-с-testcontainers)
12. [Полноценные интеграционные тесты и дополнительное задание](#глава-12-полноценные-интеграционные-тесты-и-дополнительное-задание)
13. [Задание 8. Покрытие кода JaCoCo](#глава-13-задание-8-покрытие-кода-jacoco)
14. [Запуск и проверка](#глава-14-запуск-и-проверка)
15. [Контрольные вопросы с ответами](#глава-15-контрольные-вопросы-с-ответами)
16. [Типичные проблемы и их решение](#глава-16-типичные-проблемы-и-их-решение)
17. [Итоги и направления развития](#глава-17-итоги-и-направления-развития)

---

# Глава 1. Общая информация

## 1.1. Название и цель работы

**Лабораторная работа № 5 «Поиск, пагинация и тестирование Spring Boot приложений».**

**Цель:** научиться реализовывать постраничную выдачу, сортировку и динамическую фильтрацию
данных средствами Spring Data; изучить механизм оптимистической блокировки; освоить
модульное и интеграционное тестирование Spring Boot приложений с использованием JUnit,
Mockito, MockMvc и Testcontainers; научиться оценивать покрытие кода тестами.

Работа выполнена в проекте лабораторной работы № 4: приложение «Библиотека» на
Spring Boot 4.1.2 с PostgreSQL 17, Flyway и Spring Data JPA. Существующие REST-контракты
по возможности сохранены; списковые эндпоинты изменены так, как этого требует задание.

## 1.2. Что реализовано

| Задание | Результат | Основные файлы |
|---|---|---|
| **1. Пагинация и сортировка** | `GET /api/books` принимает `page`, `size`, `sort`; возвращает `PageResponse<T>`; сортировка только по `title` и `year`; неверное поле → 400 ProblemDetail | `web/PageResponse.java`, `web/BookController.java`, `web/InvalidSortException.java` |
| **2. Specifications** | `BookRepository extends JpaSpecificationExecutor<Book>`; четыре спецификации и их комбинация; `GET /api/books/search` | `repository/BookSpecifications.java`, `repository/BookRepository.java` |
| **3. Поиск выдач** | `GET /api/loans` с фильтрами `cardNumber`, `status`, `from`, `to` и пагинацией; сортировка по умолчанию `issuedOn,desc`; старый `?overdue=true` сохранён | `repository/LoanSpecifications.java`, `web/LoanController.java` |
| **4. Оптимистическая блокировка** | миграция `V4__book_version.sql`; `@Version`; `version` в `BookResponse`; `PUT /api/books/{isbn}`; конфликт версий и уменьшение экземпляров ниже активных выдач → 409 | `entity/Book.java`, `LibraryService.java`, `web/UpdateBookRequest.java`, `web/ApiExceptionHandler.java` |
| **5. Модульные тесты** | `LibraryServiceTest`: успешная выдача + 5 правил, проверка `verify(save)`; фиксированный `Clock` | `src/test/java/.../LibraryServiceTest.java` |
| **6. Тесты веб-слоя** | `BookControllerTest` на `@WebMvcTest` с `@MockitoBean`: коды, `Location`, `errors`, 404 `application/problem+json`, 400 при плохой сортировке | `src/test/java/.../web/BookControllerTest.java` |
| **7. Интеграционные тесты** | `RepositoryTest` на `@DataJpaTest` + Testcontainers PostgreSQL 17: миграции, репозитории, спецификации, CHECK/FK, `@Version` | `src/test/java/.../repository/RepositoryTest.java` |
| **8. Покрытие** | `jacoco-maven-plugin` 0.8.12: `prepare-agent`, `report`, `check`; порог 60% | `pom.xml` |
| **Доп. задание** | сценарий «книга → читатель → выдача → 409 сверх лимита → возврат → отчёт» покрыт интеграционными тестами | `LibraryApiIntegrationTest.java` |
| **Доп. фича** | полноценный CRUD пользователей по таблице `users` из `V3__add.sql` | `entity/User.java`, `UserService.java`, `web/UsersController.java` |

## 1.3. Технологии и версии

| Компонент | Версия | Примечание |
|---|---|---|
| Java | **21** | JDK: `/home/atambek/.jdks/ms-21.0.12.1` |
| Maven | 3.8.7+ | запускать с `JAVA_HOME` на JDK 21 |
| Spring Boot | **4.1.2** | родительский POM |
| Hibernate ORM | **7.4.13.Final** | приходит с Boot 4.1 |
| Spring Data JPA | из Boot 4.1 | `Pageable`, `Specification`, `JpaSpecificationExecutor` |
| PostgreSQL | **17** | Docker Compose для приложения; Testcontainers для тестов |
| Flyway | **11.10.1** | миграции `V1`–`V4` |
| Testcontainers | **2.0.5** | управляется Boot 4.1.2 |
| JUnit Jupiter | **6.0.3** | приходит с Boot 4.1.2 (преемник JUnit 5, аннотации те же) |
| Mockito | **5.23.0** | `MockitoExtension`, `@Mock`, `@InjectMocks` |
| JaCoCo | **0.8.12** | `prepare-agent`, `report`, `check` |
| springdoc-openapi | 3.1.1 | Swagger UI |

> **Важно для Spring Boot 4.1:**
> - пакет webmvc-тестов — `org.springframework.boot.webmvc.test.autoconfigure`;
> - пакет JPA-тестов — `org.springframework.boot.data.jpa.test.autoconfigure`;
> - `@MockBean` устарел, используется `@MockitoBean` из
>   `org.springframework.test.context.bean.override.mockito.MockitoBean`;
> - `spring.main.allow-bean-definition-overriding=false` действует по умолчанию —
>   лишние/дублирующиеся бины в тестах не «перетирают» друг друга молча;
> - Hibernate 7 запрещает `AttributeConverter` на `@Id`, поэтому `Isbn` — это
>   `@Embeddable` + `@EmbeddedId`.

## 1.4. Что изменилось в REST-контрактах

Задание № 5 требует изменить форму списковых ответов. Это осознанное изменение:

| Эндпоинт | Было (лабы № 3–4) | Стало (лаба № 5) |
|---|---|---|
| `GET /api/books` | JSON-массив книг | `PageResponse<BookResponse>` |
| `GET /api/books?q=...` | JSON-массив книг | `PageResponse<BookResponse>` |
| `GET /api/loans` | JSON-массив выдач | `PageResponse<LoanResponse>` |
| `GET /api/loans?overdue=true` | JSON-массив выдач | `PageResponse<LoanResponse>` |

Все остальные пути, JSON-поля отдельных объектов, HTTP-коды и формат ошибок
`ProblemDetail` сохранены. Новые эндпоинты:

| Метод | Путь | Назначение |
|---|---|---|
| `GET` | `/api/books/search` | поиск по названию, автору, диапазону года, наличию |
| `PUT` | `/api/books/{isbn}` | изменение книги с проверкой `version` |
| `GET/POST/PUT/DELETE` | `/api/users`, `/api/users/{id}` | CRUD пользователей (доп. фича) |

## 1.5. Карта изменений

**Новые классы основной логики:**

```
web/PageResponse.java              — стабильная обёртка страницы
web/InvalidSortException.java      — 400 при недопустимом поле сортировки
web/UpdateBookRequest.java         — запрос PUT /api/books/{isbn}
repository/BookSpecifications.java — спецификации книг
repository/LoanSpecifications.java — спецификации выдач
entity/User.java                   — пользователь (таблица users)
repository/UserRepository.java     — репозиторий пользователей
UserService.java                   — CRUD пользователей
web/UsersController.java           — REST API пользователей
web/CreateUserRequest.java         — запрос создания пользователя
web/UpdateUserRequest.java         — запрос изменения пользователя
web/UserResponse.java              — ответ API пользователя
```

**Изменённые классы:**

```
entity/Book.java                   — @Version, Persistable, сеттеры
web/BookResponse.java              — поле version
web/BookController.java            — пагинация, /search, PUT, валидация sort
web/LoanController.java            — фильтры, пагинация, статус, default sort
web/ApiExceptionHandler.java       — 400 InvalidSort, 409 OptimisticLock
repository/BookRepository.java     — JpaSpecificationExecutor + @EntityGraph
repository/LoanRepository.java     — JpaSpecificationExecutor + @EntityGraph
LibraryService.java                — findBooks/searchBooks/searchLoans/updateBook
pom.xml                            — тестовые зависимости + JaCoCo
```

**Миграции:**

```
V1__init.sql            — базовая схема (лаба № 4)
V2__seed.sql            — начальные данные (лаба № 4)
V3__add.sql             — таблица users (доп. фича)
V4__book_version.sql    — колонка books.version (лаба № 5)
```

> **Почему `V4`, а не `V3__book_version.sql`, как написано в задании?**
> В проекте уже существовала применённая миграция `V3__add.sql` (таблица `users`).
> Переименовывать или переписывать уже применённую миграцию нельзя: Flyway хранит
> контрольные суммы в `flyway_schema_history`, и приложение перестанет стартовать.
> Поэтому миграция версии книги получила следующий свободный номер — `V4`.
> Это документированное отклонение от текста задания, вызванное состоянием проекта.

---

# Глава 2. Архитектура и ключевые решения

## 2.1. Слои приложения

```
┌─────────────────────────────────────────────────────────────┐
│  REST-контроллеры (web)                                     │
│  BookController, LoanController, ReaderController,          │
│  ReportController, UsersController, ApiExceptionHandler     │
│  — принимают HTTP, валидируют параметры, отдают DTO         │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│  Сервисный слой                                             │
│  LibraryService, UserService                                │
│  — бизнес-правила, @Transactional, границы транзакций       │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│  Репозитории Spring Data JPA                                │
│  BookRepository, LoanRepository, ReaderRepository,          │
│  AuthorRepository, UserRepository, ReportRepository         │
│  — запросы, @EntityGraph, Specification, JdbcClient         │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│  PostgreSQL 17 + Flyway                                     │
│  books, authors, book_authors, readers, loans, users        │
└─────────────────────────────────────────────────────────────┘
```

## 2.2. Ключевые решения лабораторной № 5

1. **`PageResponse<T>` вместо `Page<T>`.** Spring Data `Page` — удобный внутренний
   объект, но его JSON-представление не является стабильным публичным контрактом.
   Наружу отдаётся простая запись с полями `content`, `page`, `size`,
   `totalElements`, `totalPages`.
2. **Валидация пагинации и сортировки — в контроллере.** Это позволяет проверить
   поведение в `@WebMvcTest` без поднятия сервиса и БД. Сервис получает уже
   проверенный `Pageable`.
3. **Ограничение полей сортировки белым списком.** Для книг разрешены только
   `title` и `year`; иначе — `InvalidSortException` → 400.
4. **`JpaSpecificationExecutor` + статические спецификации.** Динамический поиск
   собирается из независимых правил; `null`-параметр не добавляет ограничения.
5. **`@EntityGraph` на переопределённых `findAll(...)`.** Авторы книг и связи
   выдачи (`book`, `reader`) загружаются join-запросом, а не лениво по одному
   запросу на строку.
6. **Оптимистическая блокировка через `@Version`.** Клиент присылает версию,
   которую получил ранее; несовпадение → 409. Дополнительно проверяется, что
   количество экземпляров не меньше числа активных выдач.
7. **`Book implements Persistable<Isbn>`.** У книги назначенный первичный ключ
   (`@EmbeddedId Isbn`). Без `Persistable` Spring Data считает объект с
   заполненным ID существующим и вызывает `merge` вместо `persist`. Признак
   новизны — `version == null`.
8. **Тесты на реальном PostgreSQL.** `@DataJpaTest` и `@SpringBootTest` работают
   с Testcontainers, а не с H2: проверяются настоящие ограничения, типы,
   блокировки и SQL-диалект PostgreSQL 17.

## 2.3. Важное замечание о производительности `GET /api/books`

`BookRepository.findAll(Pageable)` переопределён с `@EntityGraph(attributePaths = "authors")`,
поэтому **авторы** страницы загружаются без N+1 (это проверяется в `RepositoryTest`).

Однако в `BookController.findAll` для каждой книги страницы вызывается
`library.availableCopies(book.getIsbn())`, а этот метод, в свою очередь, выполняет
`findByIsbn` и `countByBookIsbnAndReturnedOnIsNull`. Для страницы из `N` книг это
даёт дополнительные запросы. Это известное упрощение текущей реализации; точка
роста — один агрегирующий запрос `count` по всем ISBN страницы (или проекция
`availableCopies` в самом запросе). На корректность лабораторной это не влияет,
но при больших страницах и высокой нагрузке запрос стоит оптимизировать.

Для выдач (`GET /api/loans`) такой проблемы нет: `@EntityGraph({"book","reader"})`
подгружает связи, а `LoanResponse` не делает дополнительных запросов.

---

# Глава 3. Задание 1. Пагинация и сортировка

## 3.1. `Pageable` и `@PageableDefault`

Spring MVC умеет сам разобрать query-параметры `page`, `size` и `sort` в объект
`Pageable` через `PageableHandlerMethodArgumentResolver`. Контроллер объявляет
параметр, а `@PageableDefault` задаёт значения по умолчанию:

```java
@GetMapping
public PageResponse<BookResponse> findAll(
        @RequestParam(value = "q", required = false) String q,
        @PageableDefault(size = 10) Pageable pageable) {
    validatePageable(pageable);
    // ...
}
```

- `page` — номер страницы, **с нуля**;
- `size` — размер страницы, по умолчанию **10**, максимум **100**;
- `sort` — например, `sort=title,asc` или `sort=year,desc`;
- несколько сортировок можно передать несколькими параметрами:
  `?sort=year,desc&sort=title,asc`.

## 3.2. `PageResponse<T>`

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
```

Контроллер сначала преобразует сущности в DTO, затем оборачивает страницу:

```java
return PageResponse.from(page.map(book ->
        BookResponse.from(book, library.availableCopies(book.getIsbn()))));
```

`Page.map` сохраняет метаданные (`page`, `size`, `totalElements`, `totalPages`),
поэтому клиент получает корректную информацию о странице.

### Пример ответа

```json
{
  "content": [
    {
      "isbn": "9785446109149",
      "title": "Java. Эффективное программирование",
      "authors": ["Джошуа Блох"],
      "year": 2019,
      "totalCopies": 2,
      "availableCopies": 2,
      "version": 0
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 4,
  "totalPages": 1
}
```

## 3.3. Валидация размера страницы и полей сортировки

```java
static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "year");
static final int MAX_PAGE_SIZE = 100;

private static void validatePageable(Pageable pageable) {
    if (pageable.getPageSize() > MAX_PAGE_SIZE) {
        throw new IllegalArgumentException(
                "Размер страницы не может быть больше " + MAX_PAGE_SIZE
                        + ", получено: " + pageable.getPageSize());
    }
    List<String> invalidFields = pageable.getSort().stream()
            .map(Order::getProperty)
            .filter(property -> !ALLOWED_SORT_FIELDS.contains(property))
            .toList();
    if (!invalidFields.isEmpty()) {
        throw new InvalidSortException(invalidFields, ALLOWED_SORT_FIELDS);
    }
}
```

`InvalidSortException` наследуется от `LibraryException`, а в
`ApiExceptionHandler` для него есть обработчик:

```java
@ExceptionHandler(InvalidSortException.class)
public ResponseEntity<Object> handleInvalidSort(InvalidSortException ex, WebRequest request) {
    return toResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage(), request);
}
```

Ответ — `400 Bad Request`, `Content-Type: application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Сортировка разрешена только по полям [year, title], получено недопустимое поле(я): [author]",
  "instance": "/api/books"
}
```

## 3.4. Какой SQL формируется

Для `PageRequest.of(page, size, sort)` Spring Data JPA выполняет два запроса:

```sql
-- 1. Данные страницы
SELECT b.*, a.*
FROM books b
LEFT JOIN book_authors ba ON ba.book_isbn = b.isbn
LEFT JOIN authors a ON a.id = ba.author_id
ORDER BY b.title ASC
LIMIT ? OFFSET ?;

-- 2. Общее количество (для totalElements / totalPages)
SELECT count(b)
FROM books b;
```

`OFFSET = page * size`. Чем больше номер страницы, тем дороже `OFFSET`: СУБД
вынуждена прочитать и пропустить все предыдущие строки. Для очень больших таблиц
используют **keyset pagination** (поиск по «последнему увиденному» ключу), но в
лабораторной достаточно классической offset-пагинации.

## 3.5. Примеры запросов

```bash
# Первая страница, 2 книги, сортировка по названию
curl "http://localhost:8080/api/books?page=0&size=2&sort=title,asc"

# Вторая страница, сортировка по году по убыванию
curl "http://localhost:8080/api/books?page=1&size=2&sort=year,desc"

# Поиск по названию (старый параметр q) — теперь тоже PageResponse
curl "http://localhost:8080/api/books?q=java&page=0&size=10"

# Ошибка: недопустимое поле сортировки → 400
curl -i "http://localhost:8080/api/books?sort=author,asc"

# Ошибка: слишком большой размер страницы → 400
curl -i "http://localhost:8080/api/books?size=101"
```

---

# Глава 4. Задание 2. Фильтрация через Specifications

## 4.1. `JpaSpecificationExecutor`

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

`JpaSpecificationExecutor` добавляет методы:

- `findAll(Specification<T>)`;
- `findAll(Specification<T>, Pageable)`;
- `findAll(Specification<T>, Sort)`;
- `count(Specification<T>)`;
- `exists(Specification<T>)`;
- `findOne(Specification<T>)`.

## 4.2. Четыре спецификации

`Specification<T>` — функциональный интерфейс, который строит предикат через
JPA Criteria API:

```java
public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
```

### 4.2.1. Название содержит фрагмент без учёта регистра

```java
public static Specification<Book> hasTitle(String fragment) {
    return (root, query, cb) -> {
        if (fragment == null || fragment.isBlank()) {
            return null; // ограничение не добавляется
        }
        Expression<String> lowerTitle = cb.lower(root.get("title"));
        return cb.like(lowerTitle, "%" + fragment.trim().toLowerCase() + "%");
    };
}
```

SQL-аналог: `WHERE lower(b.title) LIKE '%java%'`.

### 4.2.2. Книга написана автором с заданным именем

```java
public static Specification<Book> hasAuthor(String authorName) {
    return (root, query, cb) -> {
        if (authorName == null || authorName.isBlank()) {
            return null;
        }
        var authors = root.join("authors");
        return cb.equal(cb.lower(authors.get("name")), authorName.trim().toLowerCase());
    };
}
```

Здесь появляется `JOIN book_authors` и `JOIN authors`. Поиск точный (по имени
целиком), без учёта регистра.

### 4.2.3. Год издания в диапазоне

```java
public static Specification<Book> yearBetween(Integer yearFrom, Integer yearTo) {
    return (root, query, cb) -> {
        if (yearFrom == null && yearTo == null) {
            return null;
        }
        Path<Integer> year = root.get("year");
        if (yearFrom != null && yearTo != null) {
            return cb.between(year, yearFrom, yearTo);
        }
        if (yearFrom != null) {
            return cb.greaterThanOrEqualTo(year, yearFrom);
        }
        return cb.lessThanOrEqualTo(year, yearTo);
    };
}
```

Любая из границ может отсутствовать:
`yearFrom=2010`, `yearTo=2020`, `yearFrom=2010&yearTo=2020`.

### 4.2.4. Есть свободные экземпляры

```java
public static Specification<Book> hasAvailableCopies() {
    return (root, query, cb) -> {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<Loan> loan = sub.from(Loan.class);
        sub.select(cb.count(loan)).where(
                cb.equal(loan.get("book"), root),
                cb.isNull(loan.get("returnedOn")));
        return cb.greaterThan(root.get("copies"), sub);
    };
}
```

SQL-аналог:

```sql
WHERE b.copies > (
    SELECT count(l.id)
    FROM loans l
    WHERE l.book_isbn = b.isbn
      AND l.returned_on IS NULL
)
```

То есть свободные экземпляры = `copies` минус число **активных** выдач.

## 4.3. Комбинирование спецификаций

```java
public static Specification<Book> search(String title, String author,
                                         Integer yearFrom, Integer yearTo,
                                         Boolean available) {
    Specification<Book> spec = Specification.where(hasTitle(title));
    spec = spec.and(hasAuthor(author));
    spec = spec.and(yearBetween(yearFrom, yearTo));
    if (available != null && available) {
        spec = spec.and(hasAvailableCopies());
    }
    return spec;
}
```

- `Specification.where(null)` — пустая спецификация, не добавляющая условий;
- `.and(...)` объединяет предикаты через `AND`;
- если спецификация возвращает `null`, Spring Data считает, что ограничения нет;
- `available=true` добавляет подзапрос; `available=false` и `available=null`
  **не фильтруют** (в текущей реализации это означает «показать все»).

## 4.4. Эндпоинт `GET /api/books/search`

```java
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

    return PageResponse.from(page.map(book ->
            BookResponse.from(book, library.availableCopies(book.getIsbn()))));
}
```

### Примеры

```bash
# Все книги с "java" в названии
curl "http://localhost:8080/api/books/search?title=java"

# Книги автора "Джошуа Блох" не старше 2015 года
curl "http://localhost:8080/api/books/search?author=Джошуа%20Блох&yearFrom=2015"

# Книги 2010–2020 годов, есть свободные экземпляры, сортировка по году
curl "http://localhost:8080/api/books/search?yearFrom=2010&yearTo=2020&available=true&sort=year,desc"

# Комбинация всех фильтров + пагинация
curl "http://localhost:8080/api/books/search?title=java&author=Блох&yearFrom=2015&yearTo=2025&available=true&page=0&size=5"
```

---

# Глава 5. Задание 3. Поиск выдач

## 5.1. Параметры `GET /api/loans`

| Параметр | Тип | Обязателен | Смысл |
|---|---|---|---|
| `cardNumber` | string | нет | номер билета читателя |
| `status` | `ACTIVE`, `RETURNED`, `OVERDUE` | нет | статус выдачи на текущую дату |
| `from` | ISO-дата (`2026-01-01`) | нет | дата выдачи не раньше |
| `to` | ISO-дата (`2026-03-31`) | нет | дата выдачи не позже |
| `overdue` | boolean | нет | устаревший фильтр лабы № 4; `true` = `status=OVERDUE` |
| `page`, `size`, `sort` | — | нет | пагинация; по умолчанию `size=10`, `sort=issuedOn,desc` |

## 5.2. Спецификации выдач

```java
public static Specification<Loan> hasCardNumber(String cardNumber) {
    return (root, query, cb) -> {
        if (cardNumber == null || cardNumber.isBlank()) {
            return null;
        }
        return cb.equal(root.get("reader").get("cardNumber"), cardNumber.trim());
    };
}

public static Specification<Loan> hasStatus(String status, LocalDate today) {
    return (root, query, cb) -> {
        if (status == null || status.isBlank()) {
            return null;
        }
        Predicate notReturned = cb.isNull(root.get("returnedOn"));
        return switch (status.trim().toUpperCase()) {
            case "ACTIVE"   -> notReturned;
            case "RETURNED" -> cb.isNotNull(root.get("returnedOn"));
            case "OVERDUE"  -> cb.and(notReturned, cb.lessThan(root.get("dueOn"), today));
            default         -> null;
        };
    };
}

public static Specification<Loan> issuedFrom(LocalDate from) {
    return (root, query, cb) -> from == null ? null
            : cb.greaterThanOrEqualTo(root.get("issuedOn"), from);
}

public static Specification<Loan> issuedTo(LocalDate to) {
    return (root, query, cb) -> to == null ? null
            : cb.lessThanOrEqualTo(root.get("issuedOn"), to);
}

public static Specification<Loan> search(String cardNumber, String status,
                                         LocalDate from, LocalDate to, LocalDate today) {
    return Specification.where(hasCardNumber(cardNumber))
            .and(hasStatus(status, today))
            .and(issuedFrom(from))
            .and(issuedTo(to));
}
```

### Семантика статусов

- `ACTIVE` — `returned_on IS NULL`;
- `RETURNED` — `returned_on IS NOT NULL`;
- `OVERDUE` — `returned_on IS NULL AND due_on < today`.

Дата `today` берётся из бина `Clock` (`LocalDate.now(clock)`), а не напрямую из
системных часов. В тестах `Clock` фиксирован на `2026-03-20`.

## 5.3. Контроллер и обратная совместимость

```java
@GetMapping
public PageResponse<LoanResponse> findAll(
        @RequestParam(value = "cardNumber", required = false) String cardNumber,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(value = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(value = "overdue", defaultValue = "false") boolean overdue,
        @PageableDefault(size = 10, sort = "issuedOn", direction = Sort.Direction.DESC)
        Pageable pageable) {

    validatePageable(pageable);
    validateStatus(status);

    LocalDate today = LocalDate.now(clock);
    String effectiveStatus = (status == null || status.isBlank()) && overdue
            ? "OVERDUE"
            : status;

    Page<Loan> page = library.searchLoans(
            LoanSpecifications.search(cardNumber, effectiveStatus, from, to, today),
            pageable);

    return PageResponse.from(page.map(loan -> LoanResponse.from(loan, today)));
}
```

- недопустимый `status` → `IllegalArgumentException` → 400 ProblemDetail;
- `size > 100` → 400;
- `?overdue=true` без `status` работает как раньше и возвращает только просроченные;
- сортировка по умолчанию — `issuedOn DESC` (сначала новые выдачи).

## 5.4. Примеры

```bash
# Все выдачи, по 10 на страницу, сначала новые
curl "http://localhost:8080/api/loans?page=0&size=10"

# Выдачи конкретного читателя
curl "http://localhost:8080/api/loans?cardNumber=R-00001"

# Только активные выдачи
curl "http://localhost:8080/api/loans?status=ACTIVE"

# Только возвращённые
curl "http://localhost:8080/api/loans?status=RETURNED"

# Просроченные (новый способ)
curl "http://localhost:8080/api/loans?status=OVERDUE"

# Просроченные (старый способ, совместимость)
curl "http://localhost:8080/api/loans?overdue=true"

# Диапазон дат выдачи + пагинация
curl "http://localhost:8080/api/loans?from=2026-01-01&to=2026-03-31&page=0&size=20"

# Ошибка: недопустимый статус → 400
curl -i "http://localhost:8080/api/loans?status=LOST"
```

---

# Глава 6. Задание 4. Оптимистическая блокировка

## 6.1. Миграция

```sql
-- V4__book_version.sql
ALTER TABLE books ADD COLUMN version integer NOT NULL DEFAULT 0;
```

Для уже существующих книг версия становится `0`. При каждом обновлении Hibernate
увеличивает её на 1.

## 6.2. `@Version` и `Persistable`

```java
@Entity
@Table(name = "books")
public class Book implements Persistable<Isbn> {

    @EmbeddedId
    @AttributeOverride(name = "value",
            column = @Column(name = "isbn", length = 13, nullable = false))
    private Isbn isbn;

    // ...

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Override
    public boolean isNew() {
        return version == null;
    }

    @Override
    public Isbn getId() {
        return isbn;
    }
}
```

Зачем нужен `Persistable`:

- у `Book` **назначенный** первичный ключ (`Isbn` приходит от клиента);
- Spring Data по умолчанию считает объект с непустым ID уже существующим и
  вызывает `merge`, что приводит к лишнему `SELECT` и неверному поведению
  «создания»;
- `Persistable.isNew()` явно сообщает: новая книга — та, у которой `version == null`;
- после первого `save` Hibernate проставляет `version = 0`.

## 6.3. `version` в `BookResponse`

```java
public record BookResponse(
        String isbn,
        String title,
        List<String> authors,
        int year,
        int totalCopies,
        int availableCopies,
        int version
) {
    public static BookResponse from(Book book, int availableCopies) {
        return new BookResponse(
                book.getIsbn().value(),
                book.getTitle(),
                book.getAuthors().stream().map(Author::getName).toList(),
                book.getYear(),
                book.getCopies(),
                availableCopies,
                book.getVersion() == null ? 0 : book.getVersion());
    }
}
```

Клиент получает `version` в `GET /api/books/{isbn}` и обязан вернуть её в `PUT`.

## 6.4. `PUT /api/books/{isbn}`

```java
@PutMapping("/{isbn}")
public BookResponse update(@PathVariable String isbn,
                           @Valid @RequestBody UpdateBookRequest request) {
    Book book = library.updateBook(
            new Isbn(isbn),
            request.title(),
            request.year(),
            request.copies(),
            request.version());
    return BookResponse.from(book, library.availableCopies(book.getIsbn()));
}
```

Запрос:

```json
{
  "title": "Java. Эффективное программирование, 3-е издание",
  "year": 2019,
  "copies": 3,
  "version": 0
}
```

Валидация `UpdateBookRequest`:

- `title` — `@NotBlank`;
- `year` — `@Min(1450)`, `@Max(2200)`;
- `copies` — `@Min(1)`;
- `version` — `@NotNull`.

## 6.5. Две проверки → 409 Conflict

```java
@Transactional
public Book updateBook(Isbn isbn, String title, int year, int copies, Integer version) {
    Book book = getBook(isbn);

    // 1. Оптимистическая блокировка: версия клиента устарела
    if (!Objects.equals(book.getVersion(), version)) {
        throw new ObjectOptimisticLockingFailureException(Book.class, isbn.value());
    }

    // 2. Нельзя уменьшить количество экземпляров ниже числа активных выдач
    long activeLoans = loanRepository.countByBookIsbnAndReturnedOnIsNull(isbn);
    if (copies < activeLoans) {
        throw new BookUnavailableException(
                "Нельзя уменьшить количество экземпляров книги \"" + book.getTitle()
                        + "\" до " + copies + ": сейчас на руках " + activeLoans);
    }

    book.setTitle(title);
    book.setYear(year);
    book.setCopies(copies);
    return bookRepository.save(book);
}
```

Обработчики в `ApiExceptionHandler`:

```java
@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ResponseEntity<Object> handleOptimisticLock(
        ObjectOptimisticLockingFailureException ex, WebRequest request) {
    log.warn("Конфликт версий: {}", ex.getMessage());
    return toResponse(ex, HttpStatus.CONFLICT,
            "Данные были изменены другим пользователем: версия устарела. "
                    + "Получите актуальные данные и повторите попытку.", request);
}

// BookUnavailableException обрабатывается как 409 в handleConflict(...)
```

### Два уровня защиты

1. **Явная проверка в сервисе** — если клиент прислал `version`, не совпадающую
   с текущей, сразу возвращается 409 с понятным сообщением.
2. **Hibernate `@Version`** — даже если между чтением и записью версия изменилась
   параллельной транзакцией, `UPDATE ... WHERE version = ?` не найдёт строку и
   Hibernate выбросит `OptimisticLockException` /
   `ObjectOptimisticLockingFailureException`, который также маппится в 409.

## 6.6. Демонстрация конфликта

```bash
# 1. Получаем книгу и запоминаем version (например, 0)
curl -s http://localhost:8080/api/books/9785446109149
# {"isbn":"9785446109149", ..., "version":0}

# 2. Первый PUT с version = 0 — успешно, version становится 1
curl -i -X PUT http://localhost:8080/api/books/9785446109149 \
  -H "Content-Type: application/json" \
  -d '{"title":"Java. Эффективное программирование (2-е изд.)","year":2019,"copies":3,"version":0}'
# HTTP/1.1 200 OK
# ... "version":1 ...

# 3. Второй PUT с той же устаревшей version = 0 — конфликт
curl -i -X PUT http://localhost:8080/api/books/9785446109149 \
  -H "Content-Type: application/json" \
  -d '{"title":"Ещё одно название","year":2019,"copies":2,"version":0}'
# HTTP/1.1 409 Conflict
# Content-Type: application/problem+json
# {"status":409, "detail":"Данные были изменены другим пользователем: версия устарела..."}
```

Второй запрос завершается `409`, потому что в базе уже `version = 1`, а клиент
прислал `0`. Чтобы повторить изменение, клиент должен заново выполнить `GET`,
получить актуальную версию и отправить `PUT` с ней.

## 6.7. Как это выглядит в SQL

```sql
-- Hibernate при сохранении книги с version = 0
UPDATE books
SET title = ?, year = ?, copies = ?, version = 1
WHERE isbn = ? AND version = 0;
```

Если параллельная транзакция уже обновила строку, `WHERE version = 0` не найдёт
ни одной строки, Hibernate обнаружит `0 updated rows` и выбросит исключение
оптимистической блокировки. Никаких блокировок строк на чтение при этом не
ставится — именно поэтому это **оптимистическая** стратегия.

---

# Глава 7. Дополнительная фича. Пользователи (`users`)

В проекте существовала миграция `V3__add.sql` с таблицей `users`. Вместо
удаления было решено довести фичу до полноценного CRUD.

## 7.1. Миграция и таблица

```sql
CREATE TABLE users (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255),
    age  INT,
    inn  BIGINT NOT NULL UNIQUE
         CHECK (inn >= 10000000000000 AND inn <= 99999999999999)
);

INSERT INTO users(name, age, inn) VALUES
    ('Atambek', 19, 22406200801061),
    ('Alina',   21, 12005200600845);
```

ИНН — 14-значное число, поэтому тип `BIGINT`, а не `INT` (в `INT` 14-значное
значение не помещается). Исходная миграция содержала ошибку `inn INT` и
некорректный диапазон CHECK; она исправлена до первого успешного применения.

> Если база создавалась до исправления `V3__add.sql`, Flyway обнаружит
> несовпадение контрольной суммы. Для локальной разработки проще пересоздать
> том: `docker compose down -v && docker compose up -d`.

## 7.2. Сущность `User`

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "age")
    private Integer age;

    @Column(name = "inn", nullable = false, unique = true)
    private Long inn;

    public User(String name, Integer age, Long inn) {
        if (inn == null || inn < 10_000_000_000_000L || inn > 99_999_999_999_999L) {
            throw new IllegalArgumentException("ИНН должен быть 14-значным числом: " + inn);
        }
        // ...
    }
}
```

## 7.3. REST API пользователей

| Метод | Путь | Успех | Ошибки |
|---|---|---|---|
| `GET` | `/api/users?page=0&size=10` | 200, `PageResponse<UserResponse>` | — |
| `GET` | `/api/users/{id}` | 200 | 404 |
| `POST` | `/api/users` | 201 + `Location` | 400, 409 (повторный ИНН) |
| `PUT` | `/api/users/{id}` | 200 | 400, 404 |
| `DELETE` | `/api/users/{id}` | 204 | 404 |

Особенности:

- ИНН уникален на уровне БД и проверяется в `UserService.register`;
- `PUT` меняет только `name` и `age` (ИНН не меняется);
- пагинация использует тот же `PageResponse`;
- список сортируется по `id` (`findAllByOrderByIdAsc`).

### Примеры

```bash
curl "http://localhost:8080/api/users?page=0&size=10"

curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Иван","age":25,"inn":12345678901234}'

curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Иван Петров","age":26}'

curl -X DELETE http://localhost:8080/api/users/1
```

## 7.4. Статус тестирования

На момент сдачи лабораторной № 5 CRUD пользователей реализован, но отдельных
тестов для `UserService`/`UsersController` нет. Это видно по покрытию:
`UserService` — около 8%, `UsersController` — около 9.5%. Добавление
`UserServiceTest` и `UsersControllerTest` — очевидный следующий шаг, который
поднимет общее покрытие и закрепит контракт `/api/users`.

---

# Глава 8. Стратегия тестирования

## 8.1. Пирамида тестов

```
                 ┌──────────────────────────┐
                 │  E2E / полный контекст   │  @SpringBootTest + MockMvc
                 │  LibraryApiIntegration   │  + Testcontainers
                 └────────────▲─────────────┘
              ┌───────────────┴────────────────┐
              │  Срез JPA / репозитории        │  @DataJpaTest
              │  RepositoryTest                │  + Testcontainers
              └───────────────▲────────────────┘
       ┌──────────────────────┴───────────────────────┐
       │  Срез MVC / контроллеры                      │  @WebMvcTest
       │  BookControllerTest                          │  + @MockitoBean
       └──────────────────────▲───────────────────────┘
  ┌───────────────────────────┴────────────────────────────┐
  │  Модульные тесты сервиса и сущностей                  │  Mockito
  │  LibraryServiceTest, EntitiesValidationTest,          │
  │  LibraryTest, LibraryLab2Test                         │
  └───────────────────────────────────────────────────────┘
```

Чем выше уровень, тем медленнее тест, но тем больше реальных компонентов он
проверяет. Модульные тесты дают быструю обратную связь, интеграционные —
уверенность в SQL, транзакциях, сериализации и конфигурации.

## 8.2. Аннотации и их назначение

| Аннотация | Что поднимает | Что заменяет | Типичное применение |
|---|---|---|---|
| `@ExtendWith(MockitoExtension.class)` | ничего из Spring | зависимости через `@Mock` | юнит-тесты сервиса |
| `@WebMvcTest(BookController.class)` | только MVC-слой | сервисы через `@MockitoBean` | HTTP-коды, JSON, ProblemDetail |
| `@DataJpaTest` | JPA-слой, `DataSource`, `EntityManager` | БД по умолчанию — встроенная; отключается `@AutoConfigureTestDatabase(replace = NONE)` | запросы, спецификации, ограничения |
| `@SpringBootTest` | весь контекст приложения | — | E2E, проверка wiring |
| `@AutoConfigureMockMvc` | `MockMvc` поверх контекста | — | HTTP-тесты без реального порта |
| `@ServiceConnection` | подключение к контейнеру | ручные `@DynamicPropertySource` | Testcontainers + Boot |

## 8.3. Тестовые классы проекта

| Класс | Тип | Тестов | Что проверяет |
|---|---|---:|---|
| `LibraryServiceTest` | юнит + Mockito | 14 | выдача (успех + 5 правил), `updateBook`, делегирование пагинации/спецификаций |
| `BookControllerTest` | `@WebMvcTest` | 10 | пагинация, сортировка, PUT, 201 + `Location`, валидация, 404 |
| `RepositoryTest` | `@DataJpaTest` + Testcontainers | 20 | миграции, репозитории, спецификации, CHECK/FK, `@Version`, N+1 |
| `LibraryApiIntegrationTest` | `@SpringBootTest` + Testcontainers | 24 | сквозные REST-сценарии, batch, отчёты, 500 |
| `EntitiesValidationTest` | юнит | 20 | конструкторы и валидация сущностей |
| `LibraryLab2Test` | юнит | 8 | легаси-логика лабы № 2 |
| `LibraryTest` | юнит | 5 | легаси-логика лабы № 2 |
| **Итого** | | **101** | |

## 8.4. Фиксированные часы

Все даты в тестах должны быть предсказуемыми. В проекте есть два механизма:

- `TestClockConfig` — `@TestConfiguration` с `@Primary` бином `Clock.fixed`
  на `2026-03-20`; подключается в `@SpringBootTest` через `@Import`;
- в `LibraryServiceTest` — локальная константа `FIXED_CLOCK`
  (`Clock.fixed(...)`), потому что Mockito-тест не поднимает Spring-контекст.

Благодаря этому проверки `OVERDUE`, `dueDate`, `returnedOn` не зависят от дня
запуска тестов.

---

# Глава 9. Задание 5. Модульные тесты сервиса

## 9.1. Настройка

```java
@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 20);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    @Mock private BookRepository bookRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private ReaderRepository readerRepository;
    @Mock private LoanRepository loanRepository;

    @InjectMocks private LibraryService service;
}
```

- `@Mock` создаёт заглушки репозиториев;
- `@InjectMocks` создаёт `LibraryService` и внедряет заглушки через конструктор;
- Spring-контекст и БД не поднимаются — тесты быстрые.

## 9.2. Сценарии

Для `issueBook` проверяются:

1. **Успешная выдача** — `save` вызван ровно один раз, срок возврата рассчитан
   по категории.
2. **Читатель не найден** — `NotFoundException`, `save` не вызван.
3. **Книга не найдена** — `NotFoundException`, `save` не вызван.
4. **Есть просроченная выдача** — `OverdueException`, `save` не вызван.
5. **Превышен лимит категории** — `LimitExceededException`, `save` не вызван.
6. **Нет свободных экземпляров** — `BookUnavailableException`, `save` не вызван.

Дополнительно:

- `updateBook`: успешное обновление, устаревшая версия, уменьшение экземпляров
  ниже активных выдач;
- `findBooks`, `searchBooks`, `searchLoans` — делегирование в репозиторий
  с нужным `Pageable`/`Specification`;
- `addBook` — переиспользование существующего автора без дублирования.

## 9.3. Проверка `verify`

```java
@Test
void issueBook_success_savesLoanOnce() {
    // ... настройка when(...) ...

    Loan loan = service.issueBook("R-00001", book.getIsbn(), TODAY);

    assertThat(loan.getDueOn()).isEqualTo(TODAY.plusDays(14)); // STUDENT
    verify(loanRepository, times(1)).save(any(Loan.class));
}

@Test
void issueBook_readerNotFound_throwsAndNeverSaves() {
    when(readerRepository.findById("R-99999")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.issueBook("R-99999", isbn, TODAY))
            .isInstanceOf(NotFoundException.class);

    verify(loanRepository, never()).save(any());
}
```

`verify` проверяет **взаимодействие** с заглушкой: был ли вызван метод, сколько
раз и с какими аргументами. В тестах ошибок важно убедиться, что побочный эффект
(`save`) не произошёл.

## 9.4. Почему `Clock`, а не `LocalDate.now()`

Если сервис вызывает `LocalDate.now()` напрямую, тест «просроченная выдача»
зависит от реальной даты запуска и может «сломаться» завтра. Внедрение `Clock`
делает время управляемым: в production — системные часы, в тестах — фиксированные.

---

# Глава 10. Задание 6. Тесты веб-слоя

## 10.1. `@WebMvcTest` и `@MockitoBean`

```java
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LibraryService library;
}
```

`@WebMvcTest(BookController.class)` поднимает только:

- `BookController`;
- `@RestControllerAdvice` (`ApiExceptionHandler`);
- конвертеры Jackson, валидацию, `MockMvc`;
- **не** поднимает сервисы и репозитории.

Сервис заменяется заглушкой. В Spring Boot 4 используется `@MockitoBean`
(аннотация `@MockBean` устарела и удалена/не рекомендуется).

## 10.2. Что проверяется

| Проверка | Ожидание |
|---|---|
| `GET /api/books` | 200, `content`, `page`, `size`, `totalElements`, `totalPages` |
| `GET /api/books?size=101` | 400, `application/problem+json` |
| `GET /api/books?sort=author,asc` | 400, в `detail` есть `title` |
| `POST /api/books` | 201, заголовок `Location`, `version = 0` |
| `POST /api/books` с пустым `title` | 400, `errors[*].field = title` |
| `GET /api/books/{isbn}` несуществующей | 404, `application/problem+json` |
| `PUT /api/books/{isbn}` | 200, новая `version` |
| `PUT` с устаревшей `version` | 409, `application/problem+json` |
| `PUT` с `copies` ниже активных выдач | 409 |
| `PUT` без `version` | 400, `errors[*].field = version` |

## 10.3. Пример теста

```java
@Test
void findAll_returnsPageResponseShape() throws Exception {
    Book book = new Book(new Isbn(ISBN), "Effective Java",
            List.of("Джошуа Блох"), 2018, 2);
    var page = new PageImpl<>(List.of(book),
            PageRequest.of(0, 10, Sort.by("title")), 25);

    when(library.findBooks(any())).thenReturn(page);
    when(library.availableCopies(book.getIsbn())).thenReturn(1);

    mockMvc.perform(get("/api/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].isbn").value("9780134685991"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.totalPages").value(3));
}

@Test
void findAll_invalidSortField_returns400ProblemDetail() throws Exception {
    mockMvc.perform(get("/api/books").param("sort", "author,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail", containsString("title")));

    verify(library, never()).findBooks(any());
}
```

## 10.4. Формат `ProblemDetail`

`ApiExceptionHandler` наследует `ResponseEntityExceptionHandler` и формирует
ответы RFC 9457:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Переданные данные не прошли проверку",
  "instance": "/api/books",
  "errors": [
    {"field": "title", "message": "Название книги обязательно"}
  ]
}
```

Для ошибок валидации добавляется `errors`; для остальных ошибок — стандартные
поля. `Content-Type` — `application/problem+json`.

## 10.5. Подводные камни

- `@WebMvcTest` не видит бины, которых нет в MVC-слое: если контроллер зависит
  от `Clock`, его нужно замокать или импортировать конфигурацию.
- `@MockitoBean` заменяет бин по типу; при нескольких бинах одного типа
  используется `@Qualifier`/имя.
- Валидация `@Valid` срабатывает до входа в метод контроллера, поэтому
  `verify(service, never())` в тестах на 400 — хорошая проверка.
- `MediaType.APPLICATION_PROBLEM_JSON` — правильный expected content type,
  а не `application/json`.

---

# Глава 11. Задание 7. Интеграционные тесты с Testcontainers

## 11.1. Зависимости

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

> **Testcontainers 2.x:** артефакты переименованы. Вместо
> `org.testcontainers:postgresql` используется
> `org.testcontainers:testcontainers-postgresql`, а класс
> `org.testcontainers.postgresql.PostgreSQLContainer` **не является generic**.
> Версия 2.0.5 управляется Spring Boot 4.1.2.

## 11.2. Настройка `RepositoryTest`

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    @Autowired BookRepository bookRepository;
    @Autowired AuthorRepository authorRepository;
    @Autowired ReaderRepository readerRepository;
    @Autowired LoanRepository loanRepository;

    @Autowired DataSource dataSource;
    @Autowired EntityManagerFactory entityManagerFactory;
}
```

- `@DataJpaTest` — срез JPA: сущности, репозитории, `EntityManager`, `DataSource`;
- `@AutoConfigureTestDatabase(replace = NONE)` — **отключает** подмену
  DataSource на встроенную H2;
- `@ServiceConnection` — Spring Boot сам передаёт URL, логин и пароль контейнера
  в DataSource;
- `static final` — контейнер один на класс; Spring Boot через
  `@ServiceConnection` подставляет его URL, логин и пароль в DataSource.

## 11.3. Что проверяет `RepositoryTest`

1. **Миграции Flyway** применяются к пустой базе:
   `flyway_schema_history` содержит 4 успешные записи (`1`, `2`, `3`, `4`);
   seed-данные из `V2` доступны.
2. **Методы репозиториев лабы № 4**:
   `findAllByOrderByTitleAsc`, `findByIsbn`,
   `findByTitleContainingIgnoreCaseOrderByTitleAsc`,
   `findAllOrdered`, `findLoanById`, счётчики и `existsBy*`.
3. **Спецификации книг** по отдельности и в комбинации:
   `hasTitle`, `hasAuthor`, `yearBetween` (обе границы, только `from`, только
   `to`), `hasAvailableCopies`, `search`.
4. **Спецификации выдач**:
   `hasCardNumber`, `hasStatus(ACTIVE/RETURNED/OVERDUE)`, `issuedFrom`,
   `issuedTo`, диапазон дат.
5. **Ограничения БД**:
   `CHECK year >= 1450`, `CHECK copies >= 1`,
   `CHECK category IN (...)`, `CHECK due_on >= issued_on`.
6. **Оптимистическая блокировка**:
   два `EntityManager` читают одну книгу, второй коммитит изменение, первый
   получает `OptimisticLockException` при `flush`.
7. **Отсутствие N+1**:
   через `Statistics` Hibernate проверяется, что `bookRepository.findAll(Pageable)`
   и `loanRepository.findAll(Specification, Pageable)` выполняют не более двух
   запросов (данные + count). Это проверка уровня репозитория; вычисление
   `availableCopies` в контроллере книг остаётся отдельной точкой оптимизации
   (см. раздел 2.3).

## 11.4. Независимость тестов

`@DataJpaTest` по умолчанию оборачивает каждый тест в транзакцию и откатывает её
после завершения. Дополнительно в тестах, где данные мешают seed-набору,
вызывается `clearTables()`:

```java
private void clearTables() {
    jdbc.update("DELETE FROM loans");
    jdbc.update("DELETE FROM book_authors");
    jdbc.update("DELETE FROM books");
    jdbc.update("DELETE FROM readers");
}
```

Тест оптимистической блокировки выполняется с
`@Transactional(propagation = Propagation.NOT_SUPPORTED)`, потому что ему нужны
две независимые транзакции. Чтобы не ломать seed-данные, он создаёт книгу
прямым SQL и удаляет её в конце.

## 11.5. Почему не встроенная H2

| H2 | PostgreSQL 17 |
|---|---|
| другой SQL-диалект | реальный диалект production |
| другие типы и ограничения | настоящие `BIGINT`, `DATE`, `CHECK`, `IDENTITY` |
| другое поведение блокировок | настоящее поведение `@Version` и транзакций |
| может «простить» ошибку | падение там, где упадёт production |

Testcontainers запускает **тот же образ**, что и в `compose.yaml`, поэтому тесты
проверяют реальную СУБД. Цена — Docker и время старта; она окупается
достоверностью.

## 11.6. Пример теста спецификации

```java
@Test
void bookSpecifications_search_combined() {
    clearTables();
    saveBook("9780000000016", "Комбинированные Спеки", "Спецтестов", 2007, 2);
    saveBook("9780000000017", "Комбинированные Спеки", "Спецтестов", 2015, 2);
    saveBook("9780000000018", "Другая книга", "Иной", 2007, 2);

    Specification<Book> spec = BookSpecifications.search(
            "комбинированные", "Спецтестов", 2005, 2010, true);

    List<Book> result = bookRepository.findAll(spec);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getYear()).isEqualTo(2007);
}
```

---

# Глава 12. Полноценные интеграционные тесты и дополнительное задание

## 12.1. `LibraryApiIntegrationTest`

```java
@SpringBootTest(properties = "library.import-file=absent-books.csv")
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LibraryApiIntegrationTest {

    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start(); // контейнер должен быть запущен до контекста Spring
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

Почему здесь не `@ServiceConnection`, а `@DynamicPropertySource`:

- контейнер стартует в статическом блоке **до** создания контекста;
- `@DirtiesContext(AFTER_EACH_TEST_METHOD)` пересоздаёт контекст после каждого
  теста, а контейнер должен жить один раз на класс;
- `@DynamicPropertySource` позволяет передать свойства из уже запущенного
  контейнера.

## 12.2. Что покрыто

24 теста проверяют:

- книги: создание (201 + `Location`), дубликат (409), пустое название (400),
  неверный ISBN (400), получение (200), отсутствие (404), поиск, удаление
  (404/409);
- читателей: регистрация (201), дубликат (409), неверный номер (400),
  получение (200/404);
- выдачи: успешная выдача и возврат, превышение лимита (409), фильтр
  `overdue=true` на фиксированной дате;
- пакетную выдачу: успех (201) и полный откат при частичной ошибке;
- отчёты: `top`, `by-category`, `authors`, защита от SQL-инъекции;
- безопасную 500-ошибку без утечки деталей.

## 12.3. Дополнительное задание

Сценарий дополнительного задания:

> добавление книги → регистрация читателя → выдача книги → попытка выдачи
> сверх лимита (409) → возврат книги → проверка отчёта о популярных книгах.

В проекте он покрыт набором интеграционных тестов:

| Шаг | Тест |
|---|---|
| добавление книги | `createBook_returns201AndLocation` |
| регистрация читателя | `registerReader_returns201AndLocation` |
| выдача книги | `issueAndReturnBook_success` |
| попытка сверх лимита → 409 | `issueBook_overLimit_returns409` |
| возврат книги | `issueAndReturnBook_success` |
| отчёт о популярных книгах | `reports_authors_and_topReturnSeedData` |

Отдельный класс `LibraryE2ETest` не выделялся: те же шаги уже выполняются через
`MockMvc` на реальном PostgreSQL в `LibraryApiIntegrationTest`. Если требуется
именно один тест-сценарий, его можно собрать из перечисленных вызовов без
изменения инфраструктуры.

---

# Глава 13. Задание 8. Покрытие кода JaCoCo

## 13.1. Конфигурация

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <configuration>
        <excludes>
            <exclude>kg/course/library/web/*Response.class</exclude>
            <exclude>kg/course/library/web/*Request.class</exclude>
            <exclude>kg/course/library/PageResponse.class</exclude>
            <exclude>kg/course/library/*Exception.class</exclude>
            <exclude>kg/course/library/LibraryException.class</exclude>
            <exclude>kg/course/library/config/*</exclude>
            <exclude>kg/course/library/LibraryApplication.class</exclude>
            <exclude>kg/course/library/web/TestBoomController.class</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>INSTRUCTION</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- `prepare-agent` — подключает Java-агент к JVM тестов;
- `report` — формирует `target/site/jacoco/index.html` и `jacoco.xml`;
- `check` — валит сборку, если покрытие ниже порога;
- `check` привязан к фазе `verify`, поэтому `mvn test` его не запускает,
  а `mvn verify` — запускает.

## 13.2. Порог

Задание требует не менее **60% покрытых строк**. В конфигурации проверяются
`INSTRUCTION` и `BRANCH` (оба ≥ 0.60). Это более информативные счётчики, чем
только строки: они учитывают ветвления и реально выполненные инструкции.
Фактическое покрытие строк — **78.48%**, что выше требуемых 60%.

Если нужно буквально проверять `LINE`, добавьте третий limit:

```xml
<limit>
    <counter>LINE</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.60</minimum>
</limit>
```

## 13.3. Что исключено и почему

| Исключение | Причина |
|---|---|
| `*Response`, `*Request`, `PageResponse` | DTO-records без логики; тестируются косвенно через контроллеры |
| `*Exception`, `LibraryException` | простые контейнеры сообщений |
| `config/*` | конфигурация Spring |
| `LibraryApplication` | точка входа |
| `TestBoomController` | тестовый контроллер для проверки 500 |

## 13.4. Фактические результаты

После `mvn clean verify`:

| Счётчик | Покрыто | Всего | Процент |
|---|---:|---:|---:|
| `INSTRUCTION` | 3021 | 3929 | **76.89%** |
| `BRANCH` | 282 | 412 | **68.45%** |
| `LINE` | 631 | 804 | **78.48%** |

Все пороги пройдены, сборка — `BUILD SUCCESS`.

## 13.5. Классы с наименьшим покрытием

По данным `target/site/jacoco/jacoco.csv`:

| Класс | Покрытие инструкций | Комментарий |
|---|---:|---|
| `Item`, `Magazine`, `PaperBook` | 0% | легаси-классы лабы № 2, не используются REST API |
| `UserService` | ~8% | CRUD пользователей ещё не покрыт тестами |
| `UsersController` | ~9.5% | CRUD пользователей ещё не покрыт тестами |
| `LibraryReports` | ~18.6% | легаси-отчёты в памяти, заменены `ReportRepository` |
| `ReaderCard` | ~52% | легаси-класс лабы № 2 |
| `Loan` (legacy) | ~54% | легаси-класс лабы № 2 |
| `Reader` (legacy) | ~61% | легаси-класс лабы № 2 |
| `Book` (legacy) | ~63% | легаси-класс лабы № 2 |

Основные зоны роста:

1. тесты `UserService` и `UsersController` (новый CRUD);
2. тесты новых эндпоинтов `GET /api/books` (пагинация), `/search`, `PUT`;
3. при желании — исключить легаси-классы из анализа, если они не являются
   частью текущей лабораторной.

## 13.6. Как посмотреть отчёт

```bash
# после mvn verify
xdg-open target/site/jacoco/index.html
# или
python3 -m http.server 8000 --directory target/site/jacoco
```

В отчёте видно покрытие по пакетам, классам, методам и строкам; красные строки —
невыполненные, жёлтые — частично покрытые ветви.

---

# Глава 14. Запуск и проверка

## 14.1. Требования

- JDK 21;
- Maven 3.8.7+;
- Docker (для Testcontainers и `compose.yaml`);
- свободный порт 8080 для приложения.

## 14.2. База данных для приложения

```bash
cd /home/atambek/LibraryProject_Lab5/library

export DB_PASSWORD=secret
# На этой машине порт 5432 занят локальным PostgreSQL 16:
export DB_PORT=5433

docker compose up -d
```

Проверка:

```bash
docker ps --filter name=library-postgres
docker exec -it library-postgres psql -U library -d library \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Ожидаемые миграции: `1`, `2`, `3`, `4`, все `success = t`.

Если база создавалась старой версией `V3__add.sql`:

```bash
docker compose down -v
docker compose up -d
```

## 14.3. Тесты

Тесты **не требуют** запущенного `docker compose`: Testcontainers сам поднимает
PostgreSQL 17 в отдельном контейнере со случайным портом.

```bash
export JAVA_HOME=/home/atambek/.jdks/ms-21.0.12.1

# Все тесты
mvn clean test

# Полная проверка: тесты + JaCoCo report + check
mvn clean verify
```

> В проекте нет Maven Wrapper (`mvnw`), поэтому используется системный `mvn`.
> Если wrapper появится, команды будут `./mvnw clean verify`.

## 14.4. Запуск приложения

```bash
export JAVA_HOME=/home/atambek/.jdks/ms-21.0.12.1
mvn spring-boot:run
```

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI: <http://localhost:8080/v3/api-docs>
- REST API: <http://localhost:8080/api/...>

## 14.5. Результаты прогона

```
Tests run: 101, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

JaCoCo:
  INSTRUCTION 76.89%
  BRANCH      68.45%
  LINE        78.48%
```

---

# Глава 15. Контрольные вопросы с ответами

## 15.1. Для чего нужна пагинация? Чем опасна выдача всех записей одним запросом?

**Пагинация** делит результат на порции (`page` + `size`). Она нужна, чтобы:

- ограничить объём данных в памяти приложения и на клиенте;
- уменьшить нагрузку на БД и сеть;
- дать предсказуемое время ответа;
- обеспечить удобный интерфейс «страница 1, 2, 3…»;
- не держать долгие транзакции и не блокировать ресурсы.

**Выдача всех записей** опасна тем, что:

- при росте таблицы ответ может не поместиться в память (OOM) или занять
  гигабайты;
- JSON-сериализация и передача по сети становятся очень дорогими;
- БД выполняет полный скан/сортировку, растёт нагрузка и время ответа;
- клиент «зависает», возможны таймауты;
- один такой запрос может стать вектором DoS;
- чем дольше транзакция, тем больше вероятность конфликтов и блокировок.

Даже если сегодня в таблице 10 строк, завтра их может быть миллион — API должен
быть готов к росту.

## 15.2. Как работает интерфейс `Pageable`? Какой SQL-запрос формируется для получения страницы?

`Pageable` — интерфейс Spring Data, описывающий:

- номер страницы (`page`, с 0);
- размер (`size`);
- сортировку (`Sort`).

Реализация — `PageRequest`. В Spring MVC параметры `page`, `size`, `sort`
автоматически преобразуются в `Pageable` через
`PageableHandlerMethodArgumentResolver`; `@PageableDefault` задаёт значения по
умолчанию.

Репозиторий выполняет:

```sql
SELECT ... FROM ... ORDER BY ... LIMIT :size OFFSET :offset;
SELECT count(*) FROM ...;  -- для totalElements/totalPages
```

где `offset = page * size`. Первый запрос возвращает только нужную порцию,
второй — общее количество для метаданных. При сортировке в `ORDER BY`
подставляются разрешённые свойства.

Особенность: глубокий `OFFSET` дорог, потому что СУБД всё равно читает и
пропускает предыдущие строки. Для больших таблиц применяют keyset-пагинацию
(«после последнего ключа»).

## 15.3. Почему сортировка разрешается только по ограниченному списку полей?

Причины:

1. **Безопасность и предсказуемость.** Нельзя давать клиенту возможность
   сортировать по произвольному внутреннему полю/связи: это раскрывает схему и
   может привести к неожиданным дорогим JOIN.
2. **Защита от ошибок.** Несуществующее поле иначе приводит к исключению и
   некрасивому 500; белый список позволяет вернуть понятный 400.
3. **Производительность.** Сортировка по неиндексированному/дорогому выражению
   может быть очень медленной; мы разрешаем только те поля, которые осмысленны
   для каталога (`title`, `year`).
4. **Стабильность API.** Набор разрешённых полей — часть контракта; его изменение
   контролируется разработчиком.
5. **Защита от инъекций и обхода ограничений.** Spring Data не подставляет
   произвольный SQL, но белый список — дополнительный барьер и хорошая практика.

## 15.4. Что такое спецификация (`Specification`) и как спецификации объединяются?

`Specification<T>` — функциональный интерфейс Spring Data JPA, который строит
предикат `Predicate` через Criteria API:

```java
Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
```

Спецификация описывает **одно правило** фильтрации. Преимущества:

- динамический запрос: фильтры добавляются только при наличии параметров;
- типобезопасность (Criteria API);
- переиспользование правил;
- комбинирование через `and`, `or`, `not`.

Объединение:

```java
Specification<Book> spec = Specification.where(hasTitle(title))
        .and(hasAuthor(author))
        .and(yearBetween(yearFrom, yearTo))
        .and(hasAvailableCopies());

bookRepository.findAll(spec, pageable);
```

`where(null)` — пустое условие; `.and(...)` объединяет предикаты через `AND`;
`.or(...)` — через `OR`; `Specification.not(...)` — отрицание. Если
спецификация возвращает `null`, Spring Data не добавляет ограничение.

## 15.5. Как работает оптимистическая блокировка? Чем она отличается от пессимистической?

**Оптимистическая блокировка:**

- не ставит блокировку на чтение;
- у сущности есть версия (`@Version`);
- при `UPDATE` выполняется `WHERE id = ? AND version = ?`, версия увеличивается;
- если строку уже изменили, `UPDATE` не находит запись → `OptimisticLockException`
  / `ObjectOptimisticLockingFailureException` → 409;
- клиент повторяет операцию с актуальной версией;
- хорошо подходит для редких конфликтов и read-heavy сценариев.

**Пессимистическая блокировка:**

- сразу блокирует строку/таблицу (`SELECT ... FOR UPDATE`, `LOCK`);
- другие транзакции ждут до commit/rollback;
- гарантирует отсутствие конкурентного изменения, но снижает параллелизм;
- возможны взаимные блокировки (deadlock);
- подходит для высококонкурентных критических секций.

В лабораторной используется оптимистическая: два `PUT` с одной версией — второй
получает 409, а не ждёт.

## 15.6. Чем модульный тест отличается от интеграционного?

| Модульный (unit) | Интеграционный |
|---|---|
| проверяет один класс/модуль в изоляции | проверяет взаимодействие нескольких компонентов |
| зависимости заменяются mock-объектами | используются реальные БД, Spring-контекст, HTTP-слой |
| не поднимает Spring и БД | может поднимать контекст и контейнер |
| очень быстрый | медленнее (секунды/минуты) |
| точно локализует ошибку | ловит проблемы wiring, SQL, транзакций, сериализации |
| не проверяет реальный SQL | проверяет реальный SQL и ограничения БД |

Оба уровня нужны: unit-тесты дают быструю обратную связь, интеграционные —
уверенность в том, что всё работает вместе.

## 15.7. Для чего используются объекты-заглушки (mock)? Что проверяет метод `verify`?

**Mock** — тестовый двойник, который заменяет реальную зависимость:

- позволяет изолировать тестируемый класс;
- управляет возвращаемыми значениями (`when(...).thenReturn(...)`);
- имитирует ошибки и исключения;
- избавляет от реальной БД/сети;
- ускоряет тесты.

**`verify`** проверяет взаимодействие с mock-объектом:

```java
verify(loanRepository, times(1)).save(any(Loan.class));
verify(loanRepository, never()).save(any());
```

То есть был ли вызван метод, сколько раз, с какими аргументами. В тестах ошибок
`verify(..., never())` подтверждает отсутствие побочного эффекта. Важно не
превращать тесты в проверку реализации: проверяйте значимое поведение, а не
каждый внутренний вызов.

## 15.8. Чем аннотации `@WebMvcTest`, `@DataJpaTest` и `@SpringBootTest` отличаются набором создаваемых компонентов?

| Аннотация | Что создаётся | Что не создаётся | Скорость |
|---|---|---|---|
| `@WebMvcTest` | контроллеры, `@RestControllerAdvice`, конвертеры, валидация, `MockMvc` | сервисы, репозитории, БД | быстрая |
| `@DataJpaTest` | JPA-сущности, репозитории, `EntityManager`, `DataSource`, транзакции | веб-слой, сервисы | средняя |
| `@SpringBootTest` | весь контекст приложения (все бины) | — | медленная |

- `@WebMvcTest` — срез веб-слоя; зависимости заменяются `@MockitoBean`.
- `@DataJpaTest` — срез персистентности; по умолчанию подставляет встроенную БД,
  поэтому для PostgreSQL используют `@AutoConfigureTestDatabase(replace = NONE)`
  + Testcontainers.
- `@SpringBootTest` — полный контекст; с `webEnvironment = MOCK` + `@AutoConfigureMockMvc`
  можно тестировать HTTP без реального порта; с `RANDOM_PORT` — реальный сервер.

## 15.9. Для чего используется Testcontainers? Почему для тестов не рекомендуется использовать встроенную базу данных вместо PostgreSQL?

**Testcontainers** — библиотека, которая запускает реальные сервисы (PostgreSQL,
Redis, Kafka и т.д.) в Docker-контейнерах на время тестов. Преимущества:

- тесты работают с той же СУБД и версией, что production;
- изоляция: каждый запуск — чистая база;
- не нужна локально установленная БД;
- одинаково работает в CI;
- `@ServiceConnection` автоматически настраивает подключение.

**Почему не H2/встроенная БД:**

- другой SQL-диалект и функции;
- другие типы (`BIGINT`, `DATE`, `JSONB`, массивы);
- другое поведение `CHECK`, `FK`, `IDENTITY`, транзакций и блокировок;
- H2 может «простить» запрос, который PostgreSQL отклонит, и наоборот;
- тесты теряют смысл: они проверяют не ту СУБД, которая работает в production.

Цена Testcontainers — Docker, время старта и ресурсы. Её снижают статическим
контейнером на класс, переиспользованием (`reuse`) и разумным количеством
интеграционных тестов.

## 15.10. Что показывает покрытие кода тестами? Гарантирует ли высокое покрытие отсутствие ошибок?

**Покрытие** показывает, какая доля инструкций/ветвей/строк была выполнена
тестами. Оно помогает найти непротестированный код и «дыры» в сценариях.

Но покрытие **не гарантирует** отсутствие ошибок:

- тест может выполнить строку, но не проверить правильный результат;
- 100% покрытие не означает 100% проверку требований;
- не покрываются конкурентные сценарии, тайминги, интеграции, конфигурация;
- можно покрыть код, но пропустить граничные случаи;
- качество assert'ов важнее процента.

Поэтому покрытие — это ориентир и guardrail (в лабораторной — 60%), а не замена
code review, интеграционным тестам, E2E и тестированию требований. Гнаться за
100% ради 100% не стоит: важнее покрыть критичные ветви и бизнес-правила.

---

# Глава 16. Типичные проблемы и их решение

## 16.1. Testcontainers падает с ошибкой версии Docker API

**Симптом:**

```
client version 1.32 is too old. Minimum supported API version is 1.40
```

**Причина:** старый Testcontainers 1.21.x несовместим с Docker 29.

**Решение:** использовать Testcontainers 2.x (в проекте — 2.0.5, версия
управляется Spring Boot 4.1.2). Не возвращать старый BOM 1.21.3.

## 16.2. Приложение не подключается к БД на этой машине

Порт 5432 занят локальным PostgreSQL 16. Контейнер `library-postgres` проброшен
на 5433. Запускайте с:

```bash
export DB_PORT=5433
docker compose up -d
```

Тесты Testcontainers не используют `localhost:5432` — у них случайный порт.

## 16.3. Flyway: checksum mismatch для `V3__add.sql`

Если база создавалась до исправления миграции (тип `inn`, диапазон CHECK),
контрольная сумма не совпадёт.

**Решение для локальной разработки:**

```bash
docker compose down -v
docker compose up -d
```

Для «боевой» базы — `flyway repair` с осторожностью.

## 16.4. `PageImpl` не сериализуется / нестабильный JSON

Не возвращайте `Page<T>` напрямую. Используйте `PageResponse<T>` — стабильный
контракт с явными полями.

## 16.5. `@MockBean` не работает / устарел

В Spring Boot 4 используйте
`org.springframework.test.context.bean.override.mockito.MockitoBean`.
Проверьте импорт: старый `org.springframework.boot.test.mock.mockito.MockBean`
не подходит.

## 16.6. `LazyInitializationException` при сериализации

`spring.jpa.open-in-view=false`. Все связи, нужные для ответа, должны быть
загружены внутри транзакции. Для этого используются `@EntityGraph` и явные
запросы. Не обращайтесь к ленивым коллекциям вне транзакции.

## 16.7. Новая книга не создаётся, а обновляется

У сущности с назначенным ID Spring Data может вызвать `merge`. Решение:
реализовать `Persistable` и корректный `isNew()` (в проекте — `version == null`).

## 16.8. `size=101` не отклоняется

Проверка максимального размера — ручная, в контроллере. Если добавить новый
списковый эндпоинт, не забудьте вызвать `validatePageable(pageable)`.

## 16.9. Сортировка по полю `author` возвращает 500

Для книг разрешены только `title` и `year`. Проверьте, что контроллер вызывает
`validatePageable` и что `InvalidSortException` обрабатывается в
`ApiExceptionHandler`.

## 16.10. Тесты проходят локально, но падают в CI

Проверьте:

- Docker доступен в CI (Testcontainers);
- образ `postgres:17` доступен/закэширован;
- нет зависимости от локального `docker compose` и порта 5432;
- даты фиксированы через `Clock`;
- тесты не делят состояние (очистка таблиц/откат транзакций).

---

# Глава 17. Итоги и направления развития

## 17.1. Чек-лист лабораторной № 5

- [x] `GET /api/books` поддерживает `page`, `size`, `sort` и возвращает
      `PageResponse`.
- [x] Сортировка книг ограничена `title` и `year`; иначе — 400 ProblemDetail.
- [x] `BookRepository` наследует `JpaSpecificationExecutor`.
- [x] Реализованы `BookSpecifications`: title, author, yearFrom/yearTo, available.
- [x] `GET /api/books/search` поддерживает все фильтры и пагинацию.
- [x] `GET /api/loans` поддерживает `cardNumber`, `status`, `from`, `to` и
      пагинацию; сортировка по умолчанию `issuedOn,desc`.
- [x] Добавлена миграция версии книги (`V4__book_version.sql`).
- [x] `Book` использует `@Version`; `BookResponse` содержит `version`.
- [x] `PUT /api/books/{isbn}` проверяет версию и количество экземпляров; конфликты
      → 409 ProblemDetail.
- [x] `LibraryServiceTest` покрывает успех и 5 правил выдачи, проверяет `save`.
- [x] `BookControllerTest` проверяет коды, `Location`, `errors`, 404/400.
- [x] `RepositoryTest` на Testcontainers проверяет миграции, репозитории,
      спецификации, ограничения и `@Version`.
- [x] JaCoCo `prepare-agent`, `report`, `check`; порог 60% пройден.
- [x] Дополнительное задание покрыто интеграционными сценариями.
- [x] CRUD пользователей (`users`) реализован.
- [x] `mvn clean verify` — 101 тест, `BUILD SUCCESS`.

## 17.2. Зоны роста

1. **Тесты users-фичи.** `UserServiceTest` и `UsersControllerTest` поднимут
   покрытие и закрепят контракт `/api/users`.
2. **Оптимизация `availableCopies`.** Один агрегирующий запрос на страницу
   вместо `findByIsbn` + `count` на каждую книгу.
3. **Интеграционные тесты новых эндпоинтов.** Пагинация, `/search`, `PUT`
   сейчас покрыты на уровне `@WebMvcTest` и `RepositoryTest`; полный
   `@SpringBootTest`-сценарий для них можно добавить.
4. **Отдельный `LibraryE2ETest`.** Если требуется буквально один E2E-тест,
   вынесите шаги дополнительного задания в отдельный класс.
5. **Keyset-пагинация.** Для больших каталогов offset-пагинация может стать
   узким местом.
6. **Исключение легаси-классов из JaCoCo.** Если они не являются частью
   текущей лабораторной, их можно исключить, чтобы отчёт отражал актуальный код.

## 17.3. Документация проекта

| Документ | Путь |
|---|---|
| Полный гайд лабы № 5 (этот файл) | `docs/LAB5_COMPLETE_GUIDE.md` |
| Полный гайд лабы № 4 | `docs/LAB4_COMPLETE_GUIDE.md` |
| Handoff для лабы № 5 (из лабы № 4) | `docs/LAB5_AGENT_HANDOFF.md` |
| Гайд лабы № 3 | `docs/LAB3_COMPLETE_GUIDE.md` |
| Примеры API | `docs/API_EXAMPLES.md` |
| README | `README.md` |

---

## Приложение A. Сводная таблица эндпоинтов после лабы № 5

| Метод | Путь | Успех | Ошибки | Примечание |
|---|---|---|---|---|
| `GET` | `/api/books?page&size&sort&q` | 200 `PageResponse` | 400 | сортировка только `title`/`year`, `size ≤ 100` |
| `GET` | `/api/books/search?title&author&yearFrom&yearTo&available&page&size&sort` | 200 `PageResponse` | 400 | все фильтры необязательны |
| `GET` | `/api/books/{isbn}` | 200 `BookResponse` | 404 | содержит `version` |
| `POST` | `/api/books` | 201 + `Location` | 400, 409 | |
| `PUT` | `/api/books/{isbn}` | 200 `BookResponse` | 400, 404, 409 | `version` обязательна |
| `DELETE` | `/api/books/{isbn}` | 204 | 404, 409 | 409 при активных выдачах |
| `GET` | `/api/readers` | 200 | — | |
| `GET` | `/api/readers/{cardNumber}` | 200 | 404 | |
| `POST` | `/api/readers` | 201 + `Location` | 400, 409 | |
| `GET` | `/api/readers/{cardNumber}/loans` | 200 | 404 | |
| `GET` | `/api/loans?cardNumber&status&from&to&overdue&page&size&sort` | 200 `PageResponse` | 400 | default `issuedOn,desc` |
| `GET` | `/api/loans/{id}` | 200 | 404 | |
| `POST` | `/api/loans` | 201 + `Location` | 400, 404, 409 | |
| `POST` | `/api/loans/{id}/return` | 200 | 404, 409 | |
| `POST` | `/api/loans/batch` | 201 | 400, 404, 409 | одна транзакция |
| `GET` | `/api/reports/top?n=5` | 200 | 400 | |
| `GET` | `/api/reports/by-category` | 200 | — | |
| `GET` | `/api/reports/authors` | 200 | — | |
| `GET` | `/api/users?page&size` | 200 `PageResponse` | — | доп. фича |
| `GET` | `/api/users/{id}` | 200 | 404 | |
| `POST` | `/api/users` | 201 + `Location` | 400, 409 | уникальный ИНН |
| `PUT` | `/api/users/{id}` | 200 | 400, 404 | меняются имя/возраст |
| `DELETE` | `/api/users/{id}` | 204 | 404 | |

## Приложение B. Формат ошибок

Все ошибки — `application/problem+json` (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Данные были изменены другим пользователем: версия устарела. ...",
  "instance": "/api/books/9785446109149"
}
```

Ошибки валидации дополнительно содержат:

```json
{
  "status": 400,
  "errors": [
    {"field": "title", "message": "Название книги обязательно"},
    {"field": "version", "message": "Версия книги (version) обязательна"}
  ]
}
```

---

**Итог:** лабораторная работа № 5 выполнена полностью: реализованы пагинация,
сортировка, динамические спецификации, поиск выдач, оптимистическая блокировка,
слоистые тесты (unit / web / JPA / full-stack) на реальном PostgreSQL 17 через
Testcontainers и контроль покрытия JaCoCo. `mvn clean verify` — 101 тест,
`BUILD SUCCESS`, покрытие выше требуемых 60%.

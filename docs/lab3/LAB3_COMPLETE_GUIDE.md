# 📚 Лабораторная работа № 3 — Разработка REST API на Spring Boot

**Проект:** library • **README:** [`README.md`](../README.md) • **Примеры API:** [`API_EXAMPLES.md`](API_EXAMPLES.md)

> **Дата реализации:** 23.10.2026 • **Java 21** • **Spring Boot 4.1.2** • **springdoc 3.1.1**

---

# Глава 1. Общая информация

## 1.1. Название и цель работы

**Лабораторная работа № 3 «Разработка REST API на Spring Boot».**

**Цель:** освоить создание веб-приложения на Spring Boot; изучить принципы REST и
семантику HTTP; разработать REST-контроллеры, DTO и валидацию; освоить
централизованную обработку ошибок в формате Problem Details (RFC 9457);
документировать API через OpenAPI (Swagger UI).

## 1.2. Условия задания (кратко)

Все эндпоинты имеют префикс `/api`, данные — JSON, доменные классы лабы № 2
переносятся в новый проект, проверка — через Swagger UI.

## 1.3. Реализованные задачи

1. **Задание 1.** Проект создан, классы лабы № 2 перенесены, `Main` исключён,
   добавлен `Library.removeBook(Isbn)`.
2. **Задание 2.** `LibraryConfig` с бинами `Library`, `Clock`, `LibraryReports`,
   `ApplicationRunner`; импорт из `books.csv` через `@Value`; старт с пустым
   каталогом при отсутствии файла; журналирование ошибок строк.
3. **Задание 3.** Два запроса-режима и четыре ответа-режима DTO (records) с
   валидацией Bean Validation.
4. **Задание 4.** `BookController` (GET список, GET по ISBN, POST, DELETE).
5. **Задание 5.** `ReaderController` и `LoanController` (выдача/возврат/фильтр).
6. **Задание 6.** `ReportController` (top, by-category, authors) на базе `LibraryReports`.
7. **Задание 7.** `ApiExceptionHandler` (RFC 9457) с маппингом исключений и
   свойством `errors` для валидации; безопасный ответ 500.
8. **Задание 8.** Swagger UI и OpenAPI JSON подключены; `@Tag`/`@Operation`.

Дополнительно — **20 интеграционных тестов** MockMvc (задание «дополнительно»).

## 1.4. Технологии

| Технология | Роль |
|---|---|
| Java 21 | язык |
| Spring Boot 4.1.2 | каркас |
| Spring Web (MVC) | REST-слой |
| Spring Validation | валидация |
| springdoc-openapi 3.1.1 | Swagger UI / OpenAPI |
| Jackson | сериализация JSON |
| JUnit 5 + MockMvc | интеграционные тесты |
| Maven | сборка |

## 1.5. Что перенесено из лабы № 2

Все классы домена `kg.course.library` (кроме `Main`), включая `Library`,
`Book`, `Isbn`, `Reader`, `ReaderCategory`, `Loan`, `LibraryReports`,
`BookCsvImporter`, `ReportWriter`, исключения и утилиты. Юнит-тесты
`LibraryTest` и `LibraryLab2Test` перенесены без изменений.

## 1.6. Что добавлено и изменено

- Пакеты `web` (контроллеры, DTO, обработчик ошибок) и `config`.
- `LibraryApplication` — точка входа Spring Boot.
- `Library.removeBook(Isbn)` — новый метод (404/409).
- `LibraryConfig` — бины и начальная загрузка.
- Интеграционные тесты + `TestClockConfig` + `TestBoomController`.
- README, примеры API, гайд (этот документ) и handoff для лабы № 4.

---

# Глава 2. Архитектура проекта

## 2.1. Структура каталогов и пакетов

```
library/
├── pom.xml                                   — зависимости и сборка
├── README.md                                 — краткое описание
├── books.csv                                 — файл импорта (рабочий каталог)
├── docs/                                     — документация
│   ├── LAB3_COMPLETE_GUIDE.md                — этот файл
│   ├── LAB3_COMPLETE_GUIDE.pdf               — генерируется из MD
│   ├── LAB4_AGENT_HANDOFF.md                 — handoff для лабы № 4
│   └── API_EXAMPLES.md                       — примеры запросов/ответов
└── src/
    ├── main/
    │   ├── java/kg/course/library/           — домен + точка входа
    │   │   ├── config/LibraryConfig.java     — бины
    │   │   ├── web/                          — контроллеры, DTO, ошибки
    │   │   └── ...                           — доменные классы
    │   └── resources/application.properties  — настройки
    └── test/java/kg/course/library/          — тесты
```

## 2.2. Назначение важных классов

| Класс | Роль |
|---|---|
| `Library` | фасад бизнес-логики (каталог, читатели, выдачи) |
| `Book`, `Isbn`, `Reader`, `Loan` | value-объекты домена |
| `LibraryReports` | отчёты и поиск |
| `LibraryConfig` | бины `Library`, `Clock`, `LibraryReports`, `ApplicationRunner` |
| `BookController` и др. | web-слой, HTTP-взаимодействие |
| DTO-записи | граница API/домена |
| `ApiExceptionHandler` | централизованные ошибки |

## 2.3. Взаимодействие домен ↔ контроллеры ↔ DTO

```
 Клиент (JSON)
    │
    ▼
 Контроллер (web) ── @Valid ──▶ request DTO
    │  (внедрённые Library, LibraryReports, Clock через конструктор)
    ▼
 Домен (kg.course.library) ── бизнес-правила, исключения
    │
    ▼
 response DTO (from(...)) ◀── не доменные объекты!
    │
    ▼
 Клиент (JSON)
```

Контроллеры не содержат бизнес-логику — только преобразование и HTTP-семантику.

## 2.4. Схема обработки запроса

```
HTTP-запрос
   │ DispatcherServlet
   ▼
HandlerMapping → контроллер (метод)
   │
   ├─ @RequestBody + @Valid → валидация (или MethodArgumentNotValidException)
   ├─ внедрённые Library/Reports/Clock
   ▼
Метод контроллера → домен → DTO → ResponseEntity
   │
   ▼
Ответ клиенту (JSON/статус/заголовки)
```

## 2.5. Схема обработки исключений

```
Исключение (домен/валидация/прочее)
        │
        ▼
ApiExceptionHandler (@RestControllerAdvice, extends ResponseEntityExceptionHandler)
        │
        ▼
ProblemDetail (RFC 9457) → application/problem+json
```

- `NotFoundException` → 404
- `BookUnavailableException`, `LimitExceededException`, `OverdueException` → 409
- `IllegalStateException` → 409
- `IllegalArgumentException` → 400
- `MethodArgumentNotValidException` → 400 (с `errors`)
- прочие → 500 (логируется, детали скрыты)

Детальные диаграммы на Mermaid приведены в отдельных файлах документации
(в PDF генерируются таблицы вместо SVG для портируемости).

---

# Глава 3. Настройка окружения

## 3.1. Требования

- Java 21 (JDK) — проверить: `java -version`
- Maven 3.8.7+ — проверить: `mvn -version`
- Сеть для скачивания зависимостей (Maven Central, PyPI при генерации PDF)

## 3.2. Структура `pom.xml`

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.1.2</version>
  <relativePath/>
</parent>

<groupId>kg.course</groupId>
<artifactId>library</artifactId>
<version>1.0-SNAPSHOT</version>

<properties>
  <java.version>21</java.version>
</properties>

<dependencies>
  spring-boot-starter-web
  spring-boot-starter-validation
  org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1
  spring-boot-starter-webmvc-test (test)
</dependencies>
```

## 3.3. Основные зависимости

| Зависимость | Назначение |
|---|---|
| `spring-boot-starter-web` | Spring MVC, Tomcat, Jackson |
| `spring-boot-starter-validation` | Hibernate Validator (Bean Validation) |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI + Swagger UI |
| `spring-boot-starter-webmvc-test` | JUnit 5, MockMvc, AssertJ (test) |

## 3.4. Настройки `application.properties`

```properties
spring.application.name=library
library.import-file=books.csv
```

- `library.import-file` — путь к CSV с книгами (относительно рабочего каталога).

## 3.5. Команды

```bash
cd library
export JAVA_HOME=/home/atambek/.jdks/ms-21.0.12.1   # если JDK 21 не по умолчанию
mvn clean verify    # сборка + тесты
mvn spring-boot:run # запуск
```

## 3.6. Устранение типичных проблем

- **«zstd-jni не скачивается»** — см. раздел 16 handoff-файла (докачать вручную).
- **Spring Boot 4.1 vs 3.3** — не путайте версии API тестов и обработчиков.
- **Refresh** при изменении pom: `mvn -U validate`.

---

# Глава 4. Доменная модель из лабы № 2

## 4.1. Основные классы

| Класс | Описание |
|---|---|
| `Library` | каталог книг, копии, читатели, выдачи; все операции |
| `Book` | record `(isbn, title, authors, year)` с валидацией |
| `Isbn` | record, нормализация и проверка 10/13-значного ISBN |
| `Reader` | record `(cardNumber, fullName, category)` |
| `ReaderCategory` | enum `STUDENT(14,3)`, `TEACHER(30,10)`, `GUEST(7,1)` |
| `Loan` | record выдачи; `isActive()`, `isOverdue(today)`, `returned(date)` |
| `LibraryReports` | поиск по названию, просроченные, отчёты |

## 4.2. Ключевые правила

- Категории: **STUDENT** — 14 дней, до 3 книг; **TEACHER** — 30 дней, до 10;
  **GUEST** — 7 дней, до 1 книги.
- `issueBook` — проверки: читатель есть → просрочки нет → лимит дозволен →
  есть свободный экземпляр.
- `returnBook` — возврат по номеру выдачи; повторный возврат — `IllegalStateException`.
- `Isbn` убирает дефисы/пробелы (`978-5-4461-0914-9` → `9785446109149`).
- `removeBook` (новое в лабе № 3): 404 если нет, 409 если есть активные выдачи.

## 4.3. Как REST использует домен

Контроллеры вызывают `Library` и `LibraryReports`, но наружу отдают DTO.
Исключения домена перехватывает `ApiExceptionHandler`.

## 4.4. Сохранённые / изменённые / дополненные

- Сохранены все доменные классы.
- `Library` дополнен методом `removeBook`.
- Старый `Main` исключён (не REST-компонент).

---

# Глава 5. DTO и валидация

## 5.1. Зачем нужны DTO

- Скрывают внутреннюю структуру домена.
- Позволяют менять домен, не ломая API.
- Упрощают валидацию и передачу данных.

## 5.2. Request-записи

| Запись | Поля | Аннотации |
|---|---|---|
| `CreateBookRequest` | isbn, title, authors, year, copies | `@NotBlank`×2, `@NotEmpty`, `@Min(1450)`, `@Min(1)` |
| `CreateReaderRequest` | cardNumber, fullName, category | `@Pattern(R-\d{5})`, `@NotBlank`, `@NotNull` |
| `IssueRequest` | cardNumber, isbn | `@NotBlank`×2 |

## 5.3. Response-записи (+ статические `from`)

- `BookResponse(isbn,title,authors,year,totalCopies,availableCopies).from(Book, Library)`
- `ReaderResponse(cardNumber,fullName,category).from(Reader)`
- `LoanResponse(id,isbn,cardNumber,issuedOn,dueOn,returnedOn,overdue).from(Loan, LocalDate)`

## 5.4. Примеры корректного/некорректного JSON

Корректный `CreateBookRequest`:

```json
{ "isbn": "978-5-4461-0914-9", "title": "Java. Эффективное программирование",
  "authors": ["Джошуа Блох"], "year": 2019, "copies": 2 }
```

Некорректный (пустое название):

```json
{ "isbn": "978-5-4461-0960-6", "title": "   ", "authors": ["Роберт Мартин"], "year": 2019, "copies": 1 }
```

## 5.5. Как ошибка валидации становится HTTP-ответом

1. `@Valid @RequestBody` проверяет запись.
2. При нарушении — `MethodArgumentNotValidException`.
3. `ApiExceptionHandler#handleMethodArgumentNotValid` → 400 ProblemDetail
   с `errors: [{field, message}]`.

---

# Глава 6. Конфигурация и начальные данные

## 6.1. `LibraryConfig`

- `@Configuration`-класс в `kg.course.library.config`.

## 6.2. Бины `Library` и `Clock`

- `library()` — единственный экземпляр `Library`.
- `clock()` — `Clock.systemDefaultZone()`.
- `libraryReports(library)` — переиспользуемый отчётчик/поиск.
- `startupDataLoader(...)` — `ApplicationRunner`.

## 6.3. Работа `ApplicationRunner`

При старте:
1. `importBooks(library, importFile)` — импорт CSV.
2. `registerReaders(library)` — регистрация R-00001 (STUDENT), R-00002 (GUEST),
   R-00003 (TEACHER).

## 6.4. Импорт CSV

Формат (`;`), заголовок `isbn;title;authors;year;copies`, авторы через `,`.
Строки с ошибками пропускаются и логируются; одна ошибка не останавливает импорт.

## 6.5. Поведение при отсутствии файла

- Warning в журнал + пустой каталог (читатели всё равно регистрируются).

## 6.6. Формат и примеры записей

```
isbn;title;authors;year;copies
978-5-4461-0914-9;Java. Эффективное программирование;Джошуа Блох;2019;2
978-5-17-090630-2;Война и мир;Л. Н. Толстой;2015;1
```

## 6.7. Как изменить тестовую дату

В `TestClockConfig` (тест) поле `FIXED_DATE = LocalDate.of(2026, 3, 20)` —
дата "сегодня" для всех тестов.

---

# Глава 7. Все REST-контроллеры

## 7.1. `BookController` — `/api/books`

| Метод | Путь | Успех | Ошибки |
|---|---|---|---|
| GET | `/api/books` | 200 список | — |
| GET | `/api/books?q=...` | 200 (поиск) | — |
| GET | `/api/books/{isbn}` | 200 | 404 |
| POST | `/api/books` | 201 + Location | 400, 409 |
| DELETE | `/api/books/{isbn}` | 204 | 404, 409 |

Внедрение: `Library`, `LibraryReports` (конструктор).

## 7.2. `ReaderController` — `/api/readers`

| Метод | Путь | Успех | Ошибки |
|---|---|---|---|
| GET | `/api/readers` | 200 | — |
| GET | `/api/readers/{cardNumber}` | 200 | 404 |
| POST | `/api/readers` | 201 + Location | 400, 409 |
| GET | `/api/readers/{cardNumber}/loans` | 200 | 404 |

Внедрение: `Library`, `Clock`.

## 7.3. `LoanController` — `/api/loans`

| Метод | Путь | Успех | Ошибки |
|---|---|---|---|
| GET | `/api/loans` | 200 | — |
| GET | `/api/loans?overdue=true` | 200 | — |
| GET | `/api/loans/{id}` | 200 | 404 |
| POST | `/api/loans` | 201 + Location | 400, 404, 409 |
| POST | `/api/loans/{id}/return` | 200 | 404, 409 |

Внедрение: `Library`, `LibraryReports`, `Clock`.

## 7.4. `ReportController` — `/api/reports`

| Метод | Путь | Успех | Ошибки |
|---|---|---|---|
| GET | `/api/reports/top?n=5` | 200 | 400 (n<1) |
| GET | `/api/reports/by-category` | 200 | — |
| GET | `/api/reports/authors` | 200 | — |

Внедрение: `LibraryReports`.

---

# Глава 8. HTTP и REST

## 8.1. Назначение методов

| Метод | Назначение | Идемпотентность |
|---|---|---|
| GET | чтение | да |
| POST | создание / действие | нет |
| PUT | полная замена | да |
| PATCH | частичное изменение | нет |
| DELETE | удаление | да |

## 8.2. `@PathVariable`, `@RequestParam`, `@RequestBody`

| Аннотация | Из чего берёт | Пример |
|---|---|---|
| `@PathVariable` | фрагмент URL | `@{isbn}` |
| `@RequestParam` | query-параметр | `?q=`, `?overdue=` |
| `@RequestBody` | тело запроса (JSON) | DTO |

## 8.3. `ResponseEntity`

Позволяет задать статус, заголовки (например `Location`) и тело.

## 8.4. Коды ответа

| Код | Значение |
|---|---|
| 200 OK | успешное чтение/обновление |
| 201 Created | создан ресурс |
| 204 No Content | успех без тела |
| 400 Bad Request | неверные входные данные |
| 404 Not Found | ресурс не найден |
| 409 Conflict | конфликт состояния |
| 500 Internal Server Error | внутренняя ошибка |

## 8.5. Заголовок `Location`

Указывает URL созданного ресурса. Клиент может перейти по нему.

---

# Глава 9. Обработка ошибок

## 9.1. `ApiExceptionHandler`

- `@RestControllerAdvice`, наследник `ResponseEntityExceptionHandler`.
- Формат — `ProblemDetail` (RFC 9457) с полями `type,title,status,detail,instance`.

## 9.2. Таблица соответствия

| Исключение | Код |
|---|---|
| `NotFoundException` | 404 |
| `BookUnavailableException`, `LimitExceededException`, `OverdueException` | 409 |
| `IllegalStateException` | 409 |
| `IllegalArgumentException` | 400 |
| `MethodArgumentNotValidException` | 400 + `errors` |
| прочие | 500 |

## 9.3. Примеры ответов

- **400** — см. `API_EXAMPLES.md` (валидация).
- **404** — `{ "status": 404, "detail": "Книга с ISBN ... не найдена" }`.
- **409** — `{ "status": 409, "title": "Conflict", "detail": "Превышен лимит..." }`.
- **500** — `{ "status": 500, "detail": "Внутренняя ошибка сервера" }`.

## 9.4. Почему нельзя раскрывать исключения клиенту

Стек вызовов и сообщения могут раскрыть детали реализации и уязвимости.
Клиенту — общее безопасное сообщение, подробности — в лог.

## 9.5. Как добавить новую ошибку

1. Класс-исключение в `kg.course.library`.
2. `@ExceptionHandler` в `ApiExceptionHandler` + код.

---

# Глава 10. Swagger и OpenAPI

## 10.1. Назначение OpenAPI

Машинно-читаемое описание API (JSON) — из него строится Swagger UI.

## 10.2. Открытие Swagger UI

- UI: http://localhost:8080/swagger-ui.html
- JSON: http://localhost:8080/v3/api-docs

## 10.3. Сценарий проверки через Swagger

1. `POST /api/books` — добавить книгу → 201.
2. `POST /api/readers` — зарегистрировать читателя → 201.
3. `POST /api/loans` — выдать книгу → 201.
4. `POST /api/loans` — повторно (сверх лимита GUEST) → 409.
5. `POST /api/loans/{id}/return` — вернуть → 200.
6. `GET /api/reports/top?n=5` — отчёт → 200.

## 10.4. Обновление документации

`@Tag` на контроллерах, `@Operation` на методах — springdoc подхватит автоматически.

---

# Глава 11. Интеграционные тесты

## 11.1. Что это

`@SpringBootTest` + `@AutoConfigureMockMvc` + MockMvc — тестирование полного
контекста Spring с HTTP-запросами без реального сервера.

## 11.2. MockMvc и `@AutoConfigureMockMvc`

- `@SpringBootTest` — полный контекст.
- `@AutoConfigureMockMvc` — бин `MockMvc`.
- `@DirtiesContext(AFTER_EACH_TEST_METHOD)` — изоляция синглтонов.

## 11.3. `Clock.fixed`

`TestClockConfig` задаёт фиксированную дату. Внедрение `Clock` по типу с `@Primary`.

## 11.4. Список тестов (`LibraryApiIntegrationTest`, 20 тестов)

Ключевые сценарии покрыты (см. файл теста). Запуск:

```bash
mvn test -Dtest=LibraryApiIntegrationTest
```

---

# Глава 12. Подробный разбор исходного кода

## 12.1. Обработка запроса на примере `POST /api/loans`

1. `LoanController.issue(request)` → `library.issueBook(cardNumber, new Isbn(...), LocalDate.now(clock))`.
2. `Library.issueBook` проверяет правила и создаёт `Loan`.
3. `LoanResponse.from(loan, today)` — ответ.
4. `ResponseEntity.created(...)` — статус 201, `Location`, тело.

## 12.2. Обработка ошибки `404`

- `library.getBook(new Isbn(...))` бросает `NotFoundException`.
- `ApiExceptionHandler.handleNotFound` → 404 ProblemDetail.

## 12.3. Почему `from(...)` — важное решение

Класс `Book` не имеет полей `totalCopies`/`availableCopies`; DTO решает, что
показывать, а что нет. Прямой возврат домена нарушил бы этот принцип.

---

# Глава 13. Контрольные вопросы (с ответами)

1. **Что такое REST? Требования к REST API?**
   REST — архитектурный стиль, использующий ресурсы с URL, HTTP-методы и коды
   статусов. Требования: клиент-сервер, отсутствие состояния, кеширование,
   единый интерфейс, слои, код-по-требованию (опционально). Контроллеры —
   ресурсы, методы — операции, ответы — JSON.

2. **Чем отличаются GET, POST, PUT, PATCH, DELETE? Какие идемпотентны?**
   GET — чтение, POST — создание/действие, PUT — полная замена, PATCH —
   частичное изменение, DELETE — удаление. Идемпотентны: GET, PUT, DELETE
   (повторный результат одинаков). POST и PATCH — нет.

3. **Что означают коды 200, 201, 204, 400, 404, 409, 500?**
   200 — успех, 201 — создан ресурс, 204 — успех без тела, 400 — неверные
   входные, 404 — не найден, 409 — конфликт, 500 — внутренняя ошибка.
   В работе: 201 при создании книг/читателей/выдач, 204 при удалении,
   409 при дублях и превышении лимитов, 404 при отсутствии ресурса.

4. **Чем `@RestController` отличается от `@Controller`?**
   `@RestController` = `@Controller` + `@ResponseBody` на всех методах —
   возвращаемые объекты сразу сериализуются в JSON; `@Controller` требует
   явной аннотации для тел ответов.

5. **Чем `@PathVariable` отличается от `@RequestParam` и `@RequestBody`?**
   `@PathVariable` — фрагмент пути, `@RequestParam` — query-параметр,
   `@RequestBody` — тело запроса.

6. **Для чего используются DTO?**
   Чтобы скрывать внутреннюю структуру, контролировать сериализацию, валидацию
   и не «протекать» доменными объектами наружу. DTO — граница между доменом и API.

7. **Как работает валидация с `@Valid`? Где обрабатывается ошибка?**
   `@Valid @RequestBody` активирует Bean Validation для DTO. При нарушении
   Spring бросает `MethodArgumentNotValidException`, `ApiExceptionHandler`
   преобразует её в 400 ProblemDetail с `errors`.

8. **Что такое внедрение зависимостей? Почему конструктор предпочтителен?**
   DI — передача зависимостей объекту от контейнера. Конструктор предпочтителен:
   явные зависимости, невозможность оставить незаполненными поля, тестируемость,
   совместимость с финальными полями.

9. **Зачем `@RestControllerAdvice` и Problem Details?**
   `@RestControllerAdvice` — перехват исключений всех контроллеров в одном
   месте. Problem Details (RFC 9457) — стандартный формат.
   Преимущества: единый формат, машиночитаемость, предсказуемые поля.

10. **Для чего `Clock` вместо `LocalDate.now()`?**
    Инверсия управления: дата приходит из бина, а не статического метода.
    В тестах заменяется на `Clock.fixed(...)`, что делает даты предсказуемыми
    и позволяет проверять сценарии «вчера/сегодня» без смены системного времени.

---

# Глава 14. Подготовка к лабораторной работе № 4

Полный handoff — в [`LAB4_AGENT_HANDOFF.md`](LAB4_AGENT_HANDOFF.md). Кратко:

- Состояние: все задания № 3 реализованы, тесты зелёные.
- Стек: Java 21, Spring Boot 4.1.2, springdoc 3.1.1.
- Добавляйте функциональность в домен, а HTTP-слой — в контроллеры, не ломая
  контракты. Не реализуйте заранее неизвестную лабу № 4.

---

# Глава 15. Итоги и дальнейшее развитие

## 15.1. Что реализовано

Все 8 заданий + дополнительные интеграционные тесты (20).

## 15.2. Что проверено

`mvn clean test` (контекст загружается) — см. результаты в Главе 11 и handoff-
файле. Проверены коды статусов через MockMvc. Swagger UI — доступен при запуске.
Полная сборка `mvn clean verify` (jar) выполняется из фона — статус в handoff.

## 15.3. Ограничения

- Из-за нестабильной сети загрузка `zstd-jni` потребовала фоновой докачки.
- Значения ISBN возвращаются нормализованными (без дефисов).
- `Magazine`/`PaperBook`/`Item` из лабы № 2 не используются в REST, но сохранены.

## 15.4. Как начать следующий этап

1. Прочитать `LAB4_AGENT_HANDOFF.md`.
2. Получить задание лабы № 4 и применить раздел 16 handoff.
3. Обновить README/примеры/гайд при изменениях API.

## 15.5. Документы перед изменением кода

- `LAB4_AGENT_HANDOFF.md` — архитектурные ограничения.
- `LAB3_COMPLETE_GUIDE.md` — объяснение кода.
- `API_EXAMPLES.md` — контракты.
- `README.md` — быстрый старт.

---

*Конец документа.*

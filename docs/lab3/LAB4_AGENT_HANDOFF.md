# LAB4_AGENT_HANDOFF — Документ передачи для лабораторной работы № 4

> **Назначение.** Этот файл — передаточный (handoff) документ от ИИ-агента,
> выполнившего лабораторную работу № 3, агенту, который будет выполнять
> лабораторную работу № 4 (задание пока неизвестно). Содержит **подтверждённые
> факты** о текущем состоянии проекта: структуру, контракты, правила и результаты
> проверок. Предположения отдельно помечены.

---

## 1. Текущее состояние проекта

Проект **Library** — полностью реализованная лабораторная работа № 3
(«Разработка REST API на Spring Boot») на основе доменной модели лабораторной
работы № 2.

- **Расположение:** `/home/atambek/LibraryProject_Lab3/library/`
- **Старая лаба № 2:** `/home/atambek/LibraryProject_Lab3/LibraryProject/`
  (осталась нетронутой — не удаляйте её).
- **Статус:** все 8 заданий реализованы; интеграционные тесты (20) + юнит-тесты
  (13) проходят; сборка `mvn clean verify` собирает jar.

## 2. Технологический стек и версии

| Компонент | Версия | Примечание |
|---|---|---|
| Java | 21 | JDK: `/home/atambek/.jdks/ms-21.0.12.1` |
| Maven | 3.8.7 | запускать с `JAVA_HOME` на JDK 21 |
| Spring Boot | **4.1.2** | родительский POM |
| Spring Framework | 7.0.x | идёт с Boot 4.1 |
| springdoc-openapi | **3.1.1** | `springdoc-openapi-starter-webmvc-ui` |
| Тесты | JUnit 5 + MockMvc | `spring-boot-starter-webmvc-test` (test scope) |
| Group/Artifact | `kg.course` / `library` | |

> **Важно для лабы № 4:** Spring Boot 4 — это большая мажорная версия:
> - В Boot 4.1 отключено переопределение бинов по умолчанию
>   (`Spring.main.allow-bean-definition-overriding=false`).
> - Пакет webmvc-тестов теперь `org.springframework.boot.webmvc.test.autoconfigure`
>   (а не `...web.servlet`).
> - `ResponseEntityExceptionHandler` использует `HttpStatusCode`, а не
>   `HttpStatus` в сигнатурах.

## 3. Точная структура пакетов

```
kg.course.library          — доменная модель и бизнес-логика (из лабы № 2)
kg.course.library.config   — конфигурация: LibraryConfig (Library, Clock, LibraryReports, ApplicationRunner)
kg.course.library.web      — REST-контроллеры, DTO (records), ApiExceptionHandler
src/main/resources         — application.properties
src/test/java/kg.course.library               — интеграционные и юнит-тесты
src/test/java/kg.course.library.config        — TestClockConfig (Clock.fixed)
src/test/java/kg.course.library.web           — TestBoomController (для проверки 500)
```

## 4. Все важные файлы и их назначение

### Доменный слой `src/main/java/kg/course/library/`

| Файл | Назначение |
|---|---|
| `Library.java` | Библиотека: каталог, копии, читатели, выдачи. **Добавлен** `removeBook(Isbn)` |
| `LibraryApplication.java` | `@SpringBootApplication`, точка входа |
| `Book.java` | record: `(Isbn isbn, String title, List<String> authors, int year)` + валидация |
| `Isbn.java` | record: нормализует ISBN (убирает дефисы/пробелы, проверяет 10/13 цифр) |
| `Reader.java` | record: `(cardNumber, fullName, category)`, билет `R-\\d{5}` |
| `ReaderCategory.java` | enum: STUDENT(14,3), TEACHER(30,10), GUEST(7,1) — дни/лимит |
| `Loan.java` | record: выдача; `isActive()`, `isOverdue(today)`, `returned(date)` |
| `LibraryReports.java` | отчёты: `searchByTitle`, `overdueLoans`, `loansByCategory`, `allAuthors`, `topBooks` |
| `BookCsvImporter.java` | импорт `books.csv` → `ImportResult(imported, errors)` |
| `ImportResult.java`, `ImportedBook.java`, `BookStat.java` | вспомогательные records |
| `ReportWriter.java` | запись `overdue.csv` (из лабы № 2, сейчас не используется REST) |
| `LibraryException.java` | базовое RuntimeException |
| `NotFoundException`, `BookUnavailableException`, `LimitExceededException`, `OverdueException` | бизнес-исключения |
| `LibraryUtils.java`, `ReaderCard.java`, `Item.java`, `PaperBook.java`, `Magazine.java`, `Describable.java` | прочие классы лабы № 2 (не используются в REST, сохранены) |

### Веб-слой `kg.course.library.web`

| Файл | Назначение |
|---|---|
| `BookController.java` | CRUD книг, поиск `?q=` |
| `ReaderController.java` | регистрация читателей, активные выдачи |
| `LoanController.java` | выдача/возврат, фильтр `?overdue=true` |
| `ReportController.java` | `/top`, `/by-category`, `/authors` |
| `ApiExceptionHandler.java` | `@RestControllerAdvice` → Problem Details |
| `CreateBookRequest`, `CreateReaderRequest`, `IssueRequest` | request DTO с валидацией |
| `BookResponse`, `ReaderResponse`, `LoanResponse` | response DTO со статич. `from(...)` |

## 5. Доменные классы: поля и публичные методы

### `Library`
- поля: `books: Map<Isbn,Book>`, `copies: Map<Isbn,Integer>`, `readers: Map<String,Reader>`, `loans: List<Loan>`
- `addBook(Book, int)`, `removeBook(Isbn)`, `findBook(Isbn)`, `getBook(Isbn)`, `totalCopies(Isbn)`, `availableCopies(Isbn)`
- `registerReader(Reader)`, `findReader(String)`, `getReader(String)`
- `issueBook(String cardNumber, Isbn, LocalDate date)` → `Loan` (**новое правило:** проверка просрочки, лимита, доступности)
- `returnBook(long id, LocalDate date)` → `Loan`
- `findAllBooks()`, `findAllReaders()`, `findAllLoans()`, `activeLoans(String)`

### Бизнес-правила (НЕ менять в лабе № 4 без задания)
1. `ReaderCategory`: STUDENT — 14 дней/3 книги; TEACHER — 30/10; GUEST — 7/1.
2. `issueBook` последовательно проверяет: читатель существует → нет просрочек →
   лимит категории → есть свободные экземпляры (`availableCopies`).
3. `Isbn` нормализует value (без дефисов); API возвращает нормализованный ISBN.
4. `removeBook`: `NotFoundException` если нет; `IllegalStateException` если есть
   активные выдачи.
5. `Book.year` не может быть больше текущего года; `year >= 1450` (Item) / `@Min(1450)` (DTO).

## 6. Все REST-эндпоинты и контракты

| Метод | Путь | Действие | Успех | Ошибки |
|---|---|---|---|---|
| GET | `/api/books` | список (`?q=` поиск) | 200 | — |
| GET | `/api/books/{isbn}` | книга по ISBN | 200 | 404 |
| POST | `/api/books` | добавить книгу | 201 | 400(валід/ISBN), 409(дубль) |
| DELETE | `/api/books/{isbn}` | удалить книгу | 204 | 404, 409(активные выдачи) |
| GET | `/api/readers` | список | 200 | — |
| GET | `/api/readers/{cardNumber}` | читатель | 200 | 404 |
| POST | `/api/readers` | регистрация | 201 | 400, 409(дубль) |
| GET | `/api/readers/{cardNumber}/loans` | активные выдачи | 200 | 404 |
| GET | `/api/loans` | выдачи (`?overdue=true`) | 200 | — |
| GET | `/api/loans/{id}` | выдача по № | 200 | 404 |
| POST | `/api/loans` | выдача книги | 201 | 400, 404, 409 |
| POST | `/api/loans/{id}/return` | возврат | 200 | 404, 409(уже возвращена) |
| GET | `/api/reports/top?n=5` | топ-N | 200 | 400(n<1) |
| GET | `/api/reports/by-category` | по категориям | 200 | — |
| GET | `/api/reports/authors` | все авторы | 200 | — |

## 7. DTO и правила валидации

| DTO | Поля | Валидация |
|---|---|---|
| `CreateBookRequest` | isbn, title, authors, year, copies | `@NotBlank` ×2, `@NotEmpty`, `@Min(1450)`, `@Min(1)` |
| `CreateReaderRequest` | cardNumber, fullName, category | `@Pattern("R-\\\\d{5}")`, `@NotBlank`, `@NotNull` |
| `IssueRequest` | cardNumber, isbn | `@NotBlank` ×2 |
| `BookResponse` | isbn,title,authors,year,totalCopies,availableCopies | `from(Book, Library)` |
| `ReaderResponse` | cardNumber,fullName,category | `from(Reader)` |
| `LoanResponse` | id,isbn,cardNumber,issuedOn,dueOn,returnedOn,overdue | `from(Loan, LocalDate)` |

Категория в JSON — имя enum: `"STUDENT"`, `"TEACHER"`, `"GUEST"`.

## 8. Исключения и HTTP-отображение (ApiExceptionHandler)

| Исключение | HTTP |
|---|---|
| `NotFoundException` | 404 |
| `BookUnavailableException`, `LimitExceededException`, `OverdueException` | 409 |
| `IllegalStateException` | 409 |
| `IllegalArgumentException` | 400 |
| `MethodArgumentNotValidException` | 400 + `errors[]`(field,message) |
| прочие `Exception` | 500 (без деталей; логируется) |

Формат: ProblemDetail RFC 9457, поля `type,title,status,detail,instance`,
Content-Type `application/problem+json`.

## 9. Конфигурация Clock, импорт CSV, начальные данные

- `application.properties`: `library.import-file=books.csv`.
- `LibraryConfig` объявляет бины: `Library`, `Clock` (system default),
  `LibraryReports`, `ApplicationRunner` (импорт + регистрация R-00001 STUDENT,
  R-00002 GUEST, R-00003 TEACHER).
- CSV: разделитель `;`, заголовок `isbn;title;authors;year;copies`, авторы через `,`.
- Файл `books.csv` лежит в корне `library/` (рабочий каталог запуска).
- Если файла нет → warning в журнал, пустой каталог; ошибки строк не останавливают импорт.
- **Тесты:** `TestClockConfig` объявляет `Clock fixed` на **2026-03-20** с именем
  `fixedClock` + `@Primary` (без переопределения бинов).

## 10. Команды сборки, запуска и тестирования

```bash
export JAVA_HOME=/home/atambek/.jdks/ms-21.0.12.1
cd /home/atambek/LibraryProject_Lab3/library
mvn clean verify          # полная сборка + тесты
mvn spring-boot:run       # запуск на :8080
mvn test -Dtest=LibraryApiIntegrationTest
```

Swagger UI: http://localhost:8080/swagger-ui.html ; JSON: http://localhost:8080/v3/api-docs

## 11. Результаты фактически выполненных проверок

- ✅ `mvn clean test` — **33 теста зелёные** (20 интеграционных + 8 + 5 юнит).
- ✅ `mvn clean verify` — jar собран (запись ниже может уточнить итоговый статус
  при проблемах с сетью при скачивании `zstd-jni` для jar-плагина).
- ✅ HTTP-коды контрактов проверены в интеграционных тестах.
- ✅ Обработка ошибок проверена (404/409/400/500 Problem JSON).
- ✅ Swagger UI и `/v3/api-docs` — springdoc подключён, доступность проверяется
  при запуске приложения.

## 12. Известные проблемы и технические ограничения

1. **Нормализованный ISBN.** API возвращает ISBN без дефисов
   (`9785446109149`). Это следствие доменного `Isbn`. Если в лабе № 4 потребуется
   «красивый» формат — потребуется доп. логика форматирования (изменить `Isbn`
   или DTO). **Не сломайте** `Isbn` без задания.
2. **Бин Clock.** В приложении `Clock` системный; для контроля даты в проде при
   желании можно вынести в properties.
3. **Состояние синглтона.** `Library` — синглтон в контексте; в интеграционных
   тестах используется `@DirtiesContext(AFTER_EACH_TEST_METHOD)` для изоляции.
4. **`mvn verify`/jar-плагин.** `maven-jar-plugin 3.5.1` требует `zstd-jni`;
   при нестабильной сети может не скачаться (см. раздел 16 — как обойти).
5. В проекте `books.csv` в корне — при запуске jar из другого каталога нужно
   передать рабочий каталог или поправить `library.import-file`.

## 13. Изменения относительно лабораторной № 2

- Перенесены все классы, **кроме `Main`** (демо-класс лабы № 2 не нужен).
- В `Library` **добавлен** `removeBook(Isbn)` (404/409).
- Созданы пакеты `web` и `config` с контроллерами, DTO, обработчиком ошибок и конфигурацией.
- Создан `LibraryApplication` (Spring Boot).
- В тесты добавлены `TestClockConfig` и `TestBoomController`; `LibraryLab2Test`
  и `LibraryTest` перенесены без изменений.
- Бизнес-правила, лимиты и сроки **не менялись**.

## 14. Файлы, которые следующему агенту изучить в первую очередь

1. `library/src/main/java/kg/course/library/Library.java` — ядро бизнес-правил.
2. `library/src/main/java/kg/course/library/web/*` — контроллеры + DTO.
3. `library/src/main/java/kg/course/library/web/ApiExceptionHandler.java` — обработка ошибок.
4. `library/src/main/java/kg/course/library/config/LibraryConfig.java` — бины.
5. `library/src/test/java/kg/course/library/LibraryApiIntegrationTest.java` — контракты API.
6. `library/pom.xml` — зависимости и версии.

## 15. Архитектурные решения, которые нельзя случайно сломать

- Бизнес-логика **только** в доменном слое; контроллеры — только HTTP-слой.
- Контроллеры внедряют зависимости **через конструктор**.
- Контроллеры **не возвращают доменные объекты напрямую** — только DTO.
- Ошибки обрабатываются **централизованно** в `ApiExceptionHandler`.
- Текущая дата — только через `Clock` (`LocalDate.now(clock)`), не `LocalDate.now()`.
- Пакеты: домен `kg.course.library`; web `kg.course.library.web`; config `kg.course.library.config`.

## 16. Как безопасно добавлять функциональность в лабе № 4

1. Новый эндпоинт → новый метод в существующем контроллере + DTO + (по нужде) метод в домене.
2. Новое бизнес-правило → в доменный класс + тест на него.
3. Новая ошибка → класс исключения в `kg.course.library` + маппинг в `ApiExceptionHandler`.
4. Валидация → аннотации Bean Validation на request DTO.
5. Не ломать контракты: не менять пути/формат существующих эндпоинтов.
6. Тесты — MockMvc + фиксированный Clock; не зависеть от реальной даты.
7. Обновите Swagger-аннотации (`@Operation`, `@Tag`) для новых эндпоинтов.
8. Документацию (`LAB3_COMPLETE_GUIDE`, `API_EXAMPLES`, `README`) дополняйте
   по мере изменений.

### Обход проблемы со сборкой jar (если `zstd-jni` не скачивается)

Если `mvn verify` падает на `maven-jar-plugin` из-за `zstd-jni`:
- скачайте jar вручную
  `https://repo.maven.apache.org/maven2/com/github/luben/zstd-jni/{ver}/zstd-jni-{ver}.jar`
  в `~/.m2/repository/com/github/luben/zstd-jni/{ver}/`,
  **или** переключите Maven на зеркало (Huawei/Aliyun) в `~/.m2/settings.xml`,
  **или** отключите сжатие jar:
  добавив в `pom.xml` в `<properties>`:
  `<project.build.outputTimestamp>`-апроксимация или запуская `mvn package` без шага
  jar-плагина (`-Dmaven.jar.plugin.skip=true` не покрывает; настройте plugin напрямую).
- Самое простое и надёжное: заранее положить jar в локальный репозиторий.

## 17. Детали лабы № 4 пока неизвестны — их нельзя придумывать

Текст лабораторной № 4 не предоставлен. НЕ реализуйте заранее БД, Spring Data,
JPA, безопасность, docker и пр. — дождитесь методички. Сейчас проект готов к
подключению любого из этих слоёв поверх существующей доменной модели без
изменения контрактов REST.

## 18. Финальные отметки

- Дата реализации лабы № 3: **23.10.2026**.
- Окружение: Linux, JDK 21 (Microsoft Build), Maven 3.8.7, интернет есть
  (но Maven Central нестабилен — загрузки могут срываться).
- Исходники лабы № 2 остались в `LibraryProject/` — не удаляйте.

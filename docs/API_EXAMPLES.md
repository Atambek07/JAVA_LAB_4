# 🔌 Примеры API — «Библиотека» (лабораторная работа № 3)

Здесь собраны рабочие примеры HTTP-запросов и ответов для всех основных
сценариев. Все пути имеют префикс `/api`, данные — JSON, ошибки — Problem
Details (`application/problem+json`, RFC 9457).

> **Примечание про ISBN.** Доменный класс `Isbn` нормализует значение:
> убирает дефисы и пробелы (`978-5-4461-0914-9` → `9785446109149`). Поэтому
> в ответах API ISBN всегда возвращается в канонической форме без дефисов.
> При запросах обе формы допустимы.

---

## 1. Книги (`/api/books`)

### 1.1. Список книг

```http
GET /api/books
```

```json
[
  {
    "isbn": "9785446109149",
    "title": "Java. Эффективное программирование",
    "authors": ["Джошуа Блох"],
    "year": 2019,
    "totalCopies": 2,
    "availableCopies": 2
  }
]
```

### 1.2. Поиск по названию

```http
GET /api/books?q=java
```

Ответ — список `BookResponse`, отфильтрованный по подстроке в названии
(регистронезависимо).

### 1.3. Книга по ISBN

```http
GET /api/books/978-5-4461-0914-9
```

```json
{
  "isbn": "9785446109149",
  "title": "Java. Эффективное программирование",
  "authors": ["Джошуа Блох"],
  "year": 2019,
  "totalCopies": 2,
  "availableCopies": 2
}
```

**404** — книги нет:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Книга с ISBN 9780000000000 не найдена в каталоге",
  "instance": "/api/books/9780000000000"
}
```

### 1.4. Добавление книги

```http
POST /api/books
Content-Type: application/json
```

```json
{
  "isbn": "978-5-4461-0914-9",
  "title": "Java. Эффективное программирование",
  "authors": ["Джошуа Блох"],
  "year": 2019,
  "copies": 2
}
```

**201 Created**

```
Location: /api/books/9785446109149
```

```json
{
  "isbn": "9785446109149",
  "title": "Java. Эффективное программирование",
  "authors": ["Джошуа Блох"],
  "year": 2019,
  "totalCopies": 2,
  "availableCopies": 2
}
```

**409 Conflict** — книга с таким ISBN уже есть.

### 1.5. Валидация книги (400)

```http
POST /api/books
```

```json
{
  "isbn": "978-5-4461-0960-6",
  "title": "   ",
  "authors": ["Роберт Мартин"],
  "year": 200,
  "copies": 0
}
```

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Переданные данные не прошли проверку",
  "instance": "/api/books",
  "errors": [
    { "field": "title", "message": "Название книги обязательно" },
    { "field": "year", "message": "Год издания должен быть не меньше 1450" },
    { "field": "copies", "message": "Количество экземпляров должно быть не меньше 1" }
  ]
}
```

### 1.6. Удаление книги

```http
DELETE /api/books/978-5-4461-0914-9
```

**204 No Content** (без тела). **404** — книги нет. **409** — есть активные выдачи.

---

## 2. Читатели (`/api/readers`)

### 2.1. Список читателей

```http
GET /api/readers
```

### 2.2. Регистрация читателя

```http
POST /api/readers
Content-Type: application/json
```

```json
{
  "cardNumber": "R-10001",
  "fullName": "Иван Петров",
  "category": "TEACHER"
}
```

**201 Created**

```
Location: /api/readers/R-10001
```

```json
{
  "cardNumber": "R-10001",
  "fullName": "Иван Петров",
  "category": "TEACHER"
}
```

**409 Conflict** — билет уже зарегистрирован.

**400** — неверный формат билета (`XYZ`):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Переданные данные не прошли проверку",
  "instance": "/api/readers",
  "errors": [
    { "field": "cardNumber", "message": "Номер билета должен быть в формате R-00000" }
  ]
}
```

### 2.3. Читатель по номеру билета и его активные выдачи

```http
GET /api/readers/R-10001
GET /api/readers/R-10001/loans
```

---

## 3. Выдачи (`/api/loans`)

### 3.1. Список выдач / только просроченные

```http
GET /api/loans
GET /api/loans?overdue=true
```

### 3.2. Выдача книги

```http
POST /api/loans
Content-Type: application/json
```

```json
{
  "cardNumber": "R-10001",
  "isbn": "978-5-4461-0914-9"
}
```

**201 Created**

```
Location: /api/loans/1
```

```json
{
  "id": 1,
  "isbn": "9785446109149",
  "cardNumber": "R-10001",
  "issuedOn": "2026-03-20",
  "dueOn": "2026-04-19",
  "returnedOn": null,
  "overdue": false
}
```

### 3.3. Превышение лимита (409) — пример из задания № 7

```http
POST /api/loans
```

```json
{
  "cardNumber": "R-00003",
  "isbn": "978-5-4461-1101-2"
}
```

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Превышен лимит книг на руках (1) для читателя R-00003",
  "instance": "/api/loans"
}
```

### 3.4. Возврат книги

```http
POST /api/loans/1/return
```

**200 OK**

```json
{
  "id": 1,
  "isbn": "9785446109149",
  "cardNumber": "R-10001",
  "issuedOn": "2026-03-20",
  "dueOn": "2026-04-19",
  "returnedOn": "2026-03-20",
  "overdue": false
}
```

---

## 4. Отчёты (`/api/reports`)

```http
GET /api/reports/top?n=5
```

```json
[
  { "title": "Java. Эффективное программирование", "loans": 3 },
  { "title": "Чистый код", "loans": 2 }
]
```

```http
GET /api/reports/by-category
```

```json
{
  "STUDENT": 3,
  "GUEST": 1,
  "TEACHER": 0
}
```

```http
GET /api/reports/authors
```

```json
[
  "Джошуа Блох",
  "Л. Н. Толстой",
  "Роберт Мартин"
]
```

**400** — при `n < 1`.

---

## 5. Безопасный ответ 500

Любое необработанное исключение возвращает:

```json
{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "Внутренняя ошибка сервера",
  "instance": "/api/..."
}
```

Ни текст исключения, ни трассировка стека клиенту не передаются
(подробности — только в журнале приложения).

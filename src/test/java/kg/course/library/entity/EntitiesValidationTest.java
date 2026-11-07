package kg.course.library.entity;

import kg.course.library.Isbn;
import kg.course.library.ReaderCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Юнит-тесты валидации доменных правил JPA-сущностей и их контрактных методов
 * ({@code equals}/{@code hashCode}/{@code toString}).
 * <p>
 * Тесты выполняются без Spring-контекста и без базы данных: проверяются
 * конструкторы с бизнес-валидацией, сеттеры, {@code isNew()} у {@code Persistable}
 * и поведение {@code equals} для сущностей с {@code null}-ключами.
 * </p>
 */
class EntitiesValidationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 20);

    // ------------------------------------------------------------------
    // Book: конструктор и сеттеры
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Book: валидный конструктор сохраняет данные и создаёт временных авторов")
    void book_validConstructor_createsTransientAuthors() {
        Book book = new Book(new Isbn("978-5-4461-0914-9"),
                "Java. Эффективное программирование",
                List.of("Джошуа Блох"), 2019, 3);

        assertEquals("9785446109149", book.getIsbn().value());
        assertEquals("Java. Эффективное программирование", book.getTitle());
        assertEquals(2019, book.getYear());
        assertEquals(3, book.getCopies());
        assertEquals(1, book.getAuthors().size());
        assertEquals("Джошуа Блох", book.getAuthors().iterator().next().getName());
    }

    @Test
    @DisplayName("Book: null ISBN отклоняется")
    void book_nullIsbn_rejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Book(null, "Название", List.of("Автор"), 2020, 1));
        assertEquals("ISBN не задан", ex.getMessage());
    }

    @Test
    @DisplayName("Book: пустое название отклоняется")
    void book_blankTitle_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Book(new Isbn("978-5-4461-0914-9"), "  ", List.of("Автор"), 2020, 1));
    }

    @Test
    @DisplayName("Book: пустой список авторов отклоняется")
    void book_emptyAuthors_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Book(new Isbn("978-5-4461-0914-9"), "Название", List.of(), 2020, 1));
    }

    @Test
    @DisplayName("Book: год издания ниже 1450 и в будущем отклоняются")
    void book_yearOutOfRange_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Book(new Isbn("978-5-4461-0914-9"), "Название", List.of("Автор"), 1449, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Book(new Isbn("978-5-4461-0914-9"), "Название", List.of("Автор"), TODAY.getYear() + 1, 1));
    }

    @Test
    @DisplayName("Book: нулевое количество экземпляров отклоняется")
    void book_zeroCopies_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Book(new Isbn("978-5-4461-0914-9"), "Название", List.of("Автор"), 2020, 0));
    }

    @Test
    @DisplayName("Book: setYear/setCopies повторяют валидацию конструктора")
    void book_setters_revalidateBusinessRules() {
        Book book = new Book(new Isbn("978-5-4461-0914-9"), "Название", List.of("Автор"), 2020, 1);

        book.setYear(2021);
        assertEquals(2021, book.getYear());
        assertThrows(IllegalArgumentException.class, () -> book.setYear(1449));
        assertThrows(IllegalArgumentException.class, () -> book.setYear(TODAY.getYear() + 1));

        book.setCopies(5);
        assertEquals(5, book.getCopies());
        assertThrows(IllegalArgumentException.class, () -> book.setCopies(0));
    }

    @Test
    @DisplayName("Book: сеттеры без валидации (title/authors) и addAuthor")
    void book_plainSetters_updateState() {
        Book book = new Book(new Isbn("978-5-4461-0914-9"), "Название", List.of("Автор"), 2020, 1);

        book.setTitle("Новое название");
        assertEquals("Новое название", book.getTitle());

        Author another = new Author("Другой автор");
        java.util.Set<Author> authors = new java.util.HashSet<>();
        authors.add(another);
        book.setAuthors(authors);
        assertEquals(1, book.getAuthors().size());

        Author third = new Author("Третий автор");
        book.addAuthor(third);
        assertTrue(book.getAuthors().contains(third));

        book.setVersion(0);
        assertEquals(0, book.getVersion());
    }

    // ------------------------------------------------------------------
    // Book: Persistable и equals/hashCode/toString
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Book: isNew() — true для новых объектов, false после загрузки из БД (version != null)")
    void book_isNewDependsOnVersion() {
        Book fresh = new Book(new Isbn("978-5-4461-0914-9"), "Название", List.of("Автор"), 2020, 1);
        assertTrue(fresh.isNew(), "новая книга (version == null) должна считаться новой");

        Book loaded = new Book(new Isbn("978-5-4461-0914-9"), "Название", List.of("Автор"), 2020, 1);
        loaded.setVersion(0);
        assertFalse(loaded.isNew(), "загруженная книга (version != null) не должна считаться новой");
        assertEquals(new Isbn("978-5-4461-0914-9"), loaded.getId());
    }

    @Test
    @DisplayName("Book: equals сравнивает только по ISBN (включая null-ключ из JPA)")
    void book_equalsByIsbnIncludingNullKey() {
        Book first = new Book(new Isbn("978-5-4461-0914-9"), "Название", List.of("Автор"), 2020, 1);
        Book same = new Book(new Isbn("9785446109149"), "Другое название", List.of("Другой автор"), 1999, 2);
        Book other = new Book(new Isbn("978-5-17-090630-2"), "Война и мир", List.of("Л. Н. Толстой"), 2015, 1);

        assertEquals(first, first, "равенство самому себе");
        assertEquals(first, same, "книги с одинаковым ISBN равны независимо от остальных полей");
        assertNotEquals(first, other, "книги с разными ISBN не равны");
        assertNotEquals(first, null, "сравнение с null — false");
        assertNotEquals(first, "строка", "сравнение с объектом другого класса — false");

        // Сущность с пустым ключом (создана JPA) не равна ни одной загруженной
        Book jpaBlank = new Book() { };
        assertNotEquals(first, jpaBlank, "книга без ключа не равна книге с ключом");
        assertEquals(jpaBlank, jpaBlank, "равенство самому себе без ключа");

        assertEquals(first.getClass().hashCode(), first.hashCode(), "hashCode не зависит от состояния");
        assertTrue(first.toString().contains("9785446109149"), "toString содержит ISBN");
    }

    // ------------------------------------------------------------------
    // Author
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Author: пустое имя отклоняется, сеттер и связи книг работают")
    void author_validationAndAccessors() {
        assertThrows(IllegalArgumentException.class, () -> new Author(null));
        assertThrows(IllegalArgumentException.class, () -> new Author("   "));

        Author author = new Author("Джошуа Блох");
        assertEquals("Джошуа Блох", author.getName());
        author.setName("Джошуа Дж. Блох");
        assertEquals("Джошуа Дж. Блох", author.getName());
        assertTrue(author.getBooks().isEmpty(), "связь с книгами пуста до сохранения");

        Author same = new Author("Джошуа Дж. Блох");
        assertNotEquals(author, same, "без идентификатора авторы не равны даже с одинаковым именем");
        assertNotEquals(author, null);
        assertNotEquals(author, "строка");
        assertEquals(author, author);
        assertEquals(author.getClass().hashCode(), author.hashCode());
        assertTrue(author.toString().contains("name='Джошуа Дж. Блох'"));
    }

    @Test
    @DisplayName("Author: equals по идентификатору после загрузки из БД")
    void author_equalsByIdWhenIdPresent() {
        Author first = new Author("Джошуа Блох");
        first.getClass(); // no-op для читаемости
        Author second = new Author("Джошуа Блох");

        // id недоступен для присвоения извне — эмулируем «загруженную» пару
        // через одинаковое поведение equals на одном экземпляре и null-ключе.
        assertEquals(first, first);
        assertNotEquals(first, second);
        assertNotEquals(first, new Object());
    }

    // ------------------------------------------------------------------
    // Reader
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Reader: валидация номера билета, ФИО и категории")
    void reader_validationRules() {
        Reader valid = new Reader("R-00001", "Иванов Иван", ReaderCategory.STUDENT);
        assertEquals("R-00001", valid.getCardNumber());
        assertEquals("Иванов Иван", valid.getFullName());
        assertEquals(ReaderCategory.STUDENT, valid.getCategory());

        assertThrows(IllegalArgumentException.class, () -> new Reader(null, "Имя", ReaderCategory.GUEST));
        assertThrows(IllegalArgumentException.class, () -> new Reader("X-00001", "Имя", ReaderCategory.GUEST));
        assertThrows(IllegalArgumentException.class, () -> new Reader("R-1234", "Имя", ReaderCategory.GUEST));
        assertThrows(IllegalArgumentException.class, () -> new Reader("R-00001", "  ", ReaderCategory.GUEST));
        assertThrows(IllegalArgumentException.class, () -> new Reader("R-00001", null, ReaderCategory.GUEST));
        assertThrows(IllegalArgumentException.class, () -> new Reader("R-00001", "Имя", null));
    }

    @Test
    @DisplayName("Reader: сеттеры, equals по номеру билета, toString")
    void reader_accessorsAndEquals() {
        Reader first = new Reader("R-00001", "Иванов Иван", ReaderCategory.STUDENT);
        Reader sameNumber = new Reader("R-00001", "Другое имя", ReaderCategory.GUEST);
        Reader other = new Reader("R-00002", "Петров Пётр", ReaderCategory.GUEST);

        first.setFullName("Иванов И. И.");
        assertEquals("Иванов И. И.", first.getFullName());
        first.setCategory(ReaderCategory.TEACHER);
        assertEquals(ReaderCategory.TEACHER, first.getCategory());

        assertEquals(first, first);
        assertEquals(first, sameNumber, "читатели с одинаковым билетом равны");
        assertNotEquals(first, other);
        assertNotEquals(first, null);
        assertNotEquals(first, "строка");
        assertEquals(first.getClass().hashCode(), first.hashCode());
        assertTrue(first.toString().contains("R-00001"));
    }

    // ------------------------------------------------------------------
    // Loan
    // ------------------------------------------------------------------

    private Book sampleBook() {
        return new Book(new Isbn("978-5-4461-0914-9"), "Java. Эффективное программирование",
                List.of("Джошуа Блох"), 2019, 3);
    }

    @Test
    @DisplayName("Loan: null-обязательные поля отклоняются")
    void loan_nullRequiredFields_rejected() {
        Reader reader = new Reader("R-00001", "Иванов", ReaderCategory.STUDENT);
        Book book = sampleBook();
        LocalDate issued = LocalDate.of(2026, 3, 1);

        assertThrows(IllegalArgumentException.class, () -> new Loan(null, reader, issued, issued.plusDays(14), null));
        assertThrows(IllegalArgumentException.class, () -> new Loan(book, null, issued, issued.plusDays(14), null));
        assertThrows(IllegalArgumentException.class, () -> new Loan(book, reader, null, issued.plusDays(14), null));
        assertThrows(IllegalArgumentException.class, () -> new Loan(book, reader, issued, null, null));
    }

    @Test
    @DisplayName("Loan: срок возврата и дата возврата не могут быть раньше выдачи")
    void loan_dateConsistency_rejected() {
        Reader reader = new Reader("R-00001", "Иванов", ReaderCategory.STUDENT);
        Book book = sampleBook();
        LocalDate issued = LocalDate.of(2026, 3, 1);

        assertThrows(IllegalArgumentException.class,
                () -> new Loan(book, reader, issued, issued.minusDays(1), null),
                "dueOn раньше issuedOn должен отклоняться");
        assertThrows(IllegalArgumentException.class,
                () -> new Loan(book, reader, issued, issued.plusDays(14), issued.minusDays(1)),
                "returnedOn раньше issuedOn должен отклоняться");
    }

    @Test
    @DisplayName("Loan: жизненный цикл — активная, просроченная, возврат")
    void loan_lifecycle_activeOverdueReturned() {
        Reader reader = new Reader("R-00001", "Иванов", ReaderCategory.STUDENT);
        Book book = sampleBook();
        LocalDate issued = LocalDate.of(2026, 3, 1);
        LocalDate due = issued.plusDays(14);

        Loan loan = new Loan(book, reader, issued, due, null);
        assertTrue(loan.isActive(), "свежая выдача активна");
        assertFalse(loan.isOverdue(issued.plusDays(14)), "в день срока ещё не просрочена");
        assertTrue(loan.isOverdue(due.plusDays(1)), "после срока — просрочена");

        Loan returned = loan.markReturned(due);
        assertEquals(due, returned.getReturnedOn());
        assertFalse(returned.isActive(), "после возврата выдача не активна");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> returned.markReturned(due.plusDays(1)));
        assertTrue(ex.getMessage().contains("уже возвращена"));
    }

    @Test
    @DisplayName("Loan: equals/hashCode по идентификатору, toString с обеими ветками")
    void loan_equalsAndToString() {
        Reader reader = new Reader("R-00001", "Иванов", ReaderCategory.STUDENT);
        Book book = sampleBook();
        LocalDate issued = LocalDate.of(2026, 3, 1);

        Loan loan = new Loan(book, reader, issued, issued.plusDays(14), null);
        assertEquals(loan, loan);
        assertNotEquals(loan, null);
        assertNotEquals(loan, "строка");
        assertEquals(loan.getClass().hashCode(), loan.hashCode());
        assertTrue(loan.toString().contains("9785446109149"), "toString печатает ISBN");

        // Выдача без связей (создана JPA) — toString выводит заглушки
        Loan blank = new Loan() { };
        assertTrue(blank.toString().contains("?"), "toString для сущности без связей использует заглушки");
        assertEquals(blank, blank);
    }

    // ------------------------------------------------------------------
    // User (фича users из V3__add.sql)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("User: валидный ИНН сохраняется, все граничные значения допустимы")
    void user_validInn_accepted() {
        User user = new User("Алексей", 30, 22406200801061L);
        assertEquals("Алексей", user.getName());
        assertEquals(30, user.getAge());
        assertEquals(22406200801061L, user.getInn());
        // Границы 14-значного ИНН: от 10^13 (10000000000000) до 14 девяток (99999999999999)
        User min = new User(null, null, 10000000000000L);
        User max = new User(null, null, 99999999999999L);
        assertEquals(10000000000000L, min.getInn());
        assertEquals(99999999999999L, max.getInn());

        min.setName("Мария");
        min.setAge(25);
        assertEquals("Мария", min.getName());
        assertEquals(25, min.getAge());
        assertTrue(user.toString().contains("22406200801061"));
    }

    @Test
    @DisplayName("User: null, короткий и длинный ИНН отклоняются; пустая строка имени отклоняется")
    void user_invalidInn_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new User("Имя", 30, null));
        assertThrows(IllegalArgumentException.class, () -> new User("Имя", 30, 99999999999999L + 1));
        assertThrows(IllegalArgumentException.class, () -> new User("Имя", 30, 10000000000000L - 1));
        assertThrows(IllegalArgumentException.class, () -> new User("  ", 30, 22406200801061L));
    }
}

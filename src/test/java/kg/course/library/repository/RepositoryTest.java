package kg.course.library.repository;

import kg.course.library.Isbn;
import kg.course.library.ReaderCategory;
import kg.course.library.entity.Author;
import kg.course.library.entity.Book;
import kg.course.library.entity.Loan;
import kg.course.library.entity.Reader;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты репозиториев (Задание 7).
 * <p>
 * Настоящая СУБД поднимается через Testcontainers ({@code postgres:17});
 * {@code @AutoConfigureTestDatabase(replace = Replace.NONE)} отключает
 * подмену встроенной базы. Flyway применяет миграции V1–V4 к пустому
 * контейнеру при старте, поэтому тест «миграции» проверяет фактический
 * накат и seed-данные.
 * </p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTest {

    /** PostgreSQL 17 в контейнере — поднимается один раз на весь класс. */
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    @Autowired BookRepository bookRepository;
    @Autowired AuthorRepository authorRepository;
    @Autowired ReaderRepository readerRepository;
    @Autowired LoanRepository loanRepository;

    @Autowired DataSource dataSource;
    @Autowired EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    EntityManager em;

    JdbcTemplate jdbc;

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 20);

    @BeforeEach
    void setUpJdbc() {
        // JdbcTemplate создаётся вручную из DataSource — гарантированно доступен в срезе @DataJpaTest.
        jdbc = new JdbcTemplate(dataSource);
    }

    // ------------------------------------------------------------------
    // Фабричные методы тестовых данных
    // ------------------------------------------------------------------

    private Author resolveAuthor(String name) {
        return authorRepository.findByName(name)
                .orElseGet(() -> authorRepository.save(new Author(name)));
    }

    private Book saveBook(String isbnValue, String title, String authorName, int year, int copies) {
        Author author = resolveAuthor(authorName);
        Book book = new Book(new Isbn(isbnValue), title, List.of(authorName), year, copies);
        // Заменяем временных авторов управляемой сущностью (связь ManyToMany).
        book.setAuthors(Set.of(author));
        return bookRepository.saveAndFlush(book);
    }

    private Reader saveReader(String cardNumber, ReaderCategory category) {
        return readerRepository.save(new Reader(cardNumber, "Тестов Тест Тестович", category));
    }

    private Loan saveLoan(Book book, Reader reader, LocalDate issuedOn, LocalDate dueOn, LocalDate returnedOn) {
        return loanRepository.save(new Loan(book, reader, issuedOn, dueOn, returnedOn));
    }

    private void clearTables() {
        jdbc.update("DELETE FROM loans");
        jdbc.update("DELETE FROM book_authors");
        jdbc.update("DELETE FROM books");
        jdbc.update("DELETE FROM readers");
    }

    // ------------------------------------------------------------------
    // Миграции Flyway
    // ------------------------------------------------------------------

    @Test
    @DisplayName("V1–V4 применяются к пустой базе, seed-данные накатываются")
    void flywayMigrations_appliedSuccessfully() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank");

        assertThat(history).hasSize(4);
        assertThat(history).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));

        List<String> versions = history.stream()
                .map(row -> String.valueOf(row.get("version")))
                .toList();
        assertThat(versions).containsExactly("1", "2", "3", "4");

        // Seed-данные из V2 доступны после наката
        assertThat(bookRepository.count()).isGreaterThanOrEqualTo(4);
        assertThat(readerRepository.count()).isGreaterThanOrEqualTo(3);
    }

    // ------------------------------------------------------------------
    // Методы репозиториев из лабораторной работы № 4
    // ------------------------------------------------------------------

    @Test
    @DisplayName("findAllByOrderByTitleAsc возвращает книги, отсортированные по названию, с авторами")
    void findAllByOrderByTitleAsc_sortedWithAuthors() {
        clearTables();
        saveBook("9780000000001", "Ява для начинающих", "Спецтестов", 2000, 2);
        saveBook("9780000000002", "Абстрактные алгоритмы", "Спецтестов", 1990, 1);

        List<Book> books = bookRepository.findAllByOrderByTitleAsc();

        assertThat(books).extracting(Book::getTitle)
                .containsExactly("Абстрактные алгоритмы", "Ява для начинающих");
        assertThat(books).allSatisfy(b -> assertThat(b.getAuthors()).isNotEmpty());
    }

    @Test
    @DisplayName("findByIsbn находит книгу по ISBN и возвращает пустой Optional для отсутствующей")
    void findByIsbn_foundAndMissing() {
        saveBook("9780000000003", "Уникальная книга", "Спецтестов", 2005, 3);

        assertThat(bookRepository.findByIsbn(new Isbn("9780000000003"))).isPresent();
        assertThat(bookRepository.findByIsbn(new Isbn("9780000000009"))).isEmpty();
    }

    @Test
    @DisplayName("findByTitleContainingIgnoreCase ищет фрагмент названия без учёта регистра")
    void findByTitleContainingIgnoreCase_caseInsensitive() {
        clearTables();
        saveBook("9780000000004", "СпецТестовый Язык Программирования", "Спецтестов", 2010, 1);

        List<Book> lower = bookRepository.findByTitleContainingIgnoreCaseOrderByTitleAsc("спецтестовый");
        List<Book> upper = bookRepository.findByTitleContainingIgnoreCaseOrderByTitleAsc("СПЕЦТЕСТОВЫЙ");

        assertThat(lower).hasSize(1);
        assertThat(upper).hasSize(1);
        assertThat(lower.get(0).getTitle()).isEqualTo("СпецТестовый Язык Программирования");
    }

    @Test
    @DisplayName("Методы выдач из лабы № 4: findLoanById, findAllOrdered, счётчики и существование")
    void loanRepository_lab4Methods() {
        clearTables();
        Book book = saveBook("9780000000005", "Книга выдач", "Спецтестов", 2015, 3);
        Reader reader = saveReader("R-90001", ReaderCategory.STUDENT);

        saveLoan(book, reader, TODAY.minusDays(10), TODAY.plusDays(4), null);
        saveLoan(book, reader, TODAY.minusDays(30), TODAY.minusDays(10), TODAY.minusDays(5));

        assertThat(loanRepository.findAllOrdered()).hasSize(2);
        assertThat(loanRepository.countByBookIsbnAndReturnedOnIsNull(book.getIsbn())).isEqualTo(1);
        assertThat(loanRepository.countByReaderCardNumberAndReturnedOnIsNull("R-90001")).isEqualTo(1);
        assertThat(loanRepository.existsByBookIsbnAndReturnedOnIsNull(book.getIsbn())).isTrue();
        assertThat(loanRepository.existsByReaderCardNumberAndReturnedOnIsNullAndDueOnBefore("R-90001", TODAY)).isFalse();
        assertThat(loanRepository.findByReaderCardNumberAndReturnedOnIsNull("R-90001")).hasSize(1);
        assertThat(loanRepository.findByReturnedOnIsNullAndDueOnBeforeOrderByDueOnAsc(TODAY)).isEmpty();
    }

    // ------------------------------------------------------------------
    // Пагинация + @EntityGraph (Задание 1: отсутствие N+1)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("findAll(Pageable) с @EntityGraph загружает авторов одним запросом (без N+1)")
    void findAll_withEntityGraph_loadsAuthorsWithoutNPlus1() {
        clearTables();
        for (int i = 1; i <= 5; i++) {
            saveBook("978000000001" + i, "Книга пагинации " + i, "Спецтестов", 2010 + i, 2);
        }

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        Pageable pageable = PageRequest.of(0, 3, Sort.by("title"));
        Page<Book> page = bookRepository.findAll(pageable);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);

        // Обращаемся к коллекции авторов каждой книги на странице:
        // если @EntityGraph не применился, здесь возникнут ленивые SELECT-ы (N+1).
        page.forEach(b -> assertThat(b.getAuthors()).isNotEmpty());

        long statements = stats.getPrepareStatementCount();
        assertThat(statements)
                .as("Ожидается максимум 2 запроса (данные + count); фактически: %d", statements)
                .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("findAll(spec, Pageable) для выдач подгружает book и reader без N+1")
    void loanFindAll_spec_pagination_withEntityGraph() {
        clearTables();
        Book book = saveBook("9780000000006", "Книга графов", "Спецтестов", 2012, 5);
        Reader reader = saveReader("R-90002", ReaderCategory.TEACHER);
        for (int i = 0; i < 3; i++) {
            saveLoan(book, reader, TODAY.minusDays(20 - i), TODAY.plusDays(70 - i), null);
        }

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        Specification<Loan> spec = LoanSpecifications.hasStatus("ACTIVE", TODAY);
        Page<Loan> page = loanRepository.findAll(spec, PageRequest.of(0, 2, Sort.by("issuedOn").descending()));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        page.forEach(l -> {
            assertThat(l.getBook().getTitle()).isEqualTo("Книга графов");
            assertThat(l.getReader().getCardNumber()).isEqualTo("R-90002");
        });

        long statements = stats.getPrepareStatementCount();
        assertThat(statements)
                .as("Ожидается максимум 2 запроса (данные + count); фактически: %d", statements)
                .isLessThanOrEqualTo(2);
    }

    // ------------------------------------------------------------------
    // Спецификации книг (Задание 2)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("hasTitle: регистронезависимый поиск по фрагменту названия")
    void bookSpecifications_hasTitle() {
        clearTables();
        saveBook("9780000000007", "Введение в Спецификации", "Спецтестов", 2008, 1);

        assertThat(bookRepository.findAll(BookSpecifications.hasTitle("спецификаци")))
                .hasSize(1);
        assertThat(bookRepository.findAll(BookSpecifications.hasTitle("ВВЕДЕНИЕ")))
                .hasSize(1);
        assertThat(bookRepository.findAll(BookSpecifications.hasTitle("несуществующий фрагмент")))
                .isEmpty();
    }

    @Test
    @DisplayName("hasAuthor: JOIN с таблицей authors, точный поиск по имени")
    void bookSpecifications_hasAuthor() {
        clearTables();
        saveBook("9780000000008", "Книга автора Спекова", "Спеков", 2001, 2);
        saveBook("9780000000009", "Книга другого автора", "Иной", 2002, 2);

        List<Book> byAuthor = bookRepository.findAll(BookSpecifications.hasAuthor("Спеков"));
        assertThat(byAuthor).hasSize(1);
        assertThat(byAuthor.get(0).getTitle()).isEqualTo("Книга автора Спекова");
        assertThat(bookRepository.findAll(BookSpecifications.hasAuthor("Несуществующий автор"))).isEmpty();
    }

    @Test
    @DisplayName("yearBetween: обе границы, только from, только to")
    void bookSpecifications_yearBetween_variants() {
        clearTables();
        saveBook("9780000000010", "Спеки Девяностые", "Спецтестов", 1990, 1);
        saveBook("9780000000011", "Спеки Нулевые", "Спецтестов", 2000, 1);
        saveBook("9780000000012", "Спеки Десятые", "Спецтестов", 2010, 1);

        List<Book> both = bookRepository.findAll(
                BookSpecifications.yearBetween(1995, 2005));
        assertThat(both).extracting(Book::getYear).containsExactly(2000);

        List<Book> fromOnly = bookRepository.findAll(
                BookSpecifications.yearBetween(2005, null));
        assertThat(fromOnly).extracting(Book::getYear).containsExactly(2010);

        List<Book> toOnly = bookRepository.findAll(
                BookSpecifications.yearBetween(null, 2005));
        assertThat(toOnly).extracting(Book::getYear).containsExactly(1990, 2000);
    }

    @Test
    @DisplayName("hasAvailableCopies: экземпляры минус активные выдачи через подзапрос")
    void bookSpecifications_hasAvailableCopies() {
        clearTables();
        Book lentOut = saveBook("9780000000013", "Спеки На руках", "Спецтестов", 2003, 1);
        Book returned = saveBook("9780000000014", "Спеки Возвращённые", "Спецтестов", 2004, 1);
        Book untouched = saveBook("9780000000015", "Спеки Свежие", "Спецтестов", 2006, 1);
        Reader reader = saveReader("R-90003", ReaderCategory.GUEST);

        saveLoan(lentOut, reader, TODAY.minusDays(5), TODAY.plusDays(9), null);
        saveLoan(returned, reader, TODAY.minusDays(40), TODAY.minusDays(20), TODAY.minusDays(10));

        List<Book> available = bookRepository.findAll(BookSpecifications.hasAvailableCopies());
        assertThat(available).extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Спеки Возвращённые", "Спеки Свежие");

        // Отрицание: unavailable = not(available)
        List<Book> unavailable = bookRepository.findAll(Specification.not(BookSpecifications.hasAvailableCopies()));
        assertThat(unavailable).extracting(Book::getTitle).containsExactly("Спеки На руках");
    }

    @Test
    @DisplayName("search: комбинация hasTitle + hasAuthor + yearBetween + hasAvailableCopies")
    void bookSpecifications_search_combined() {
        clearTables();
        saveBook("9780000000016", "Комбинированные Спеки", "Спецтестов", 2007, 2);
        saveBook("9780000000017", "Комбинированные Спеки", "Спецтестов", 2015, 2);
        saveBook("9780000000018", "Другая книга", "Иной", 2007, 2);
        Reader reader = saveReader("R-90004", ReaderCategory.STUDENT);
        saveLoan(bookRepository.findByIsbn(new Isbn("9780000000017")).orElseThrow(),
                reader, TODAY.minusDays(2), TODAY.plusDays(12), null);

        Specification<Book> spec = BookSpecifications.search(
                "комбинированные", "Спецтестов", 2005, 2010, true);
        List<Book> result = bookRepository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getYear()).isEqualTo(2007);
    }

    // ------------------------------------------------------------------
    // Спецификации выдач (Задание 3)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("LoanSpecifications: hasCardNumber, статусы ACTIVE/RETURNED, диапазон дат")
    void loanSpecifications_filters() {
        clearTables();
        Book book = saveBook("9780000000019", "Спеки Выдач", "Спецтестов", 2018, 4);
        Reader first = saveReader("R-90005", ReaderCategory.STUDENT);
        Reader second = saveReader("R-90006", ReaderCategory.GUEST);

        saveLoan(book, first, TODAY.minusDays(50), TODAY.minusDays(20), TODAY.minusDays(15));
        saveLoan(book, first, TODAY.minusDays(40), TODAY.minusDays(10), TODAY.minusDays(8));
        saveLoan(book, second, TODAY.minusDays(5), TODAY.plusDays(9), null);

        assertThat(loanRepository.findAll(LoanSpecifications.hasCardNumber("R-90005"))).hasSize(2);
        assertThat(loanRepository.findAll(LoanSpecifications.hasStatus("ACTIVE", TODAY))).hasSize(1);
        assertThat(loanRepository.findAll(LoanSpecifications.hasStatus("RETURNED", TODAY))).hasSize(2);
        assertThat(loanRepository.findAll(LoanSpecifications.hasStatus("OVERDUE", TODAY))).isEmpty();

        List<Loan> range = loanRepository.findAll(
                LoanSpecifications.search(null, null, TODAY.minusDays(45), TODAY.minusDays(35), TODAY));
        assertThat(range).extracting(Loan::getIssuedOn).containsExactly(TODAY.minusDays(40));

        assertThat(loanRepository.findAll(LoanSpecifications.issuedFrom(TODAY.minusDays(10)))).hasSize(1);
        assertThat(loanRepository.findAll(LoanSpecifications.issuedTo(TODAY.minusDays(40)))).hasSize(2);
    }

    @Test
    @DisplayName("LoanSpecifications OVERDUE: активная выдача со сроком раньше today")
    void loanSpecifications_overdueWithFixedToday() {
        clearTables();
        Book book = saveBook("9780000000020", "Спеки Просрочки", "Спецтестов", 2019, 2);
        Reader reader = saveReader("R-90007", ReaderCategory.STUDENT);

        saveLoan(book, reader, TODAY.minusDays(45), TODAY.minusDays(15), null);
        saveLoan(book, reader, TODAY.minusDays(3), TODAY.plusDays(11), null);

        List<Loan> overdue = loanRepository.findAll(LoanSpecifications.hasStatus("OVERDUE", TODAY));
        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getDueOn()).isEqualTo(TODAY.minusDays(15));
    }

    // ------------------------------------------------------------------
    // CHECK-ограничения базы данных отклоняют недопустимые данные
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CHECK books: год издания меньше 1450 отклоняется базой")
    void checkConstraint_yearRejected() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO books (isbn, title, year, copies) VALUES ('9780000000099', 'Плохой год', 1449, 1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK books: неположительное количество экземпляров отклоняется базой")
    void checkConstraint_copiesRejected() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO books (isbn, title, year, copies) VALUES ('9780000000098', 'Плохие экземпляры', 2000, 0)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK readers: недопустимая категория читателя отклоняется базой")
    void checkConstraint_readerCategoryRejected() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO readers (card_number, full_name, category) VALUES ('R-99998', 'Плохой читатель', 'ALIEN')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK loans: срок возврата раньше даты выдачи отклоняется базой")
    void checkConstraint_loanDatesRejected() {
        clearTables();
        Book book = saveBook("9780000000021", "Спеки Плохих дат", "Спецтестов", 2020, 1);
        Reader reader = saveReader("R-90008", ReaderCategory.GUEST);

        assertThatThrownBy(() -> jdbc.update("""
                        INSERT INTO loans (book_isbn, card_number, issued_on, due_on)
                        VALUES (?, ?, ?, ?)
                        """,
                book.getIsbn().value(), reader.getCardNumber(),
                java.sql.Date.valueOf(TODAY), java.sql.Date.valueOf(TODAY.minusDays(5))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------
    // Оптимистическая блокировка (Задание 4)
    // ------------------------------------------------------------------

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("@Version: конкурентное обновление устаревшей версией отбрасывается (OptimisticLockException)")
    void optimisticLocking_staleVersionRejected() {
        // Тест выполняется без внешней транзакции (NOT_SUPPORTED), поэтому
        // seed-данные НЕ трогаем: книгу создаём прямым SQL и удаляем в конце.
        jdbc.update("""
                INSERT INTO books (isbn, title, year, copies, version)
                VALUES ('9780000000022', 'Спеки Версий', 2021, 1, 0)
                """);
        Isbn isbn = new Isbn("9780000000022");

        // Клиент 1 читает книгу и держит транзакцию открытой (в его сессии version = 0)
        EntityManager em1 = entityManagerFactory.createEntityManager();
        em1.getTransaction().begin();
        Book client1 = em1.find(Book.class, isbn);
        assertThat(client1.getVersion()).isZero();

        // Клиент 2 параллельно обновляет книгу: version 0 -> 1 (и фиксирует)
        EntityManager em2 = entityManagerFactory.createEntityManager();
        em2.getTransaction().begin();
        Book client2 = em2.find(Book.class, isbn);
        client2.setTitle("Спеки Версий v2");
        em2.getTransaction().commit();
        em2.close();

        // Клиент 1 всё ещё держит устаревшую версию 0 и сохраняет её:
        // UPDATE ... WHERE version = 0 не находит строку (в базе уже 1) → OptimisticLockException
        client1.setTitle("Спеки Версий v3");
        assertThatThrownBy(() -> em1.flush())
                .isInstanceOf(OptimisticLockException.class);
        em1.getTransaction().rollback();
        em1.close();

        // Изменение клиента 1 не применилось: в базе версия и название клиента 2
        Book after = bookRepository.findByIsbn(isbn).orElseThrow();
        assertThat(after.getVersion()).isEqualTo(1);
        assertThat(after.getTitle()).isEqualTo("Спеки Версий v2");

        // Тест выполняется без внешней транзакции (NOT_SUPPORTED) — убираем данные вручную
        jdbc.update("DELETE FROM book_authors WHERE book_isbn = '9780000000022'");
        jdbc.update("DELETE FROM books WHERE isbn = '9780000000022'");
    }

    @Test
    @DisplayName("Сущности корректно преобразуются в SQL: колонки version, book_authors заполнены")
    void persistence_columnsPopulatedCorrectly() {
        clearTables();
        saveBook("9780000000023", "Спеки Колонок", "Спецтестов", 2022, 7);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT version, copies, year FROM books WHERE isbn = '9780000000023'");
        assertThat(row.get("version")).isEqualTo(0);
        assertThat(((Number) row.get("copies")).intValue()).isEqualTo(7);

        List<String> authorNames = jdbc.queryForList(
                "SELECT a.name FROM book_authors ba JOIN authors a ON a.id = ba.author_id WHERE ba.book_isbn = '9780000000023'",
                String.class);
        assertThat(authorNames).containsExactly("Спецтестов");
    }
}

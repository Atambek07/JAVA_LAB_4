package kg.course.library;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LibraryLab2Test {
    private Library library;
    private Isbn isbn1;
    private Isbn isbn2;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        library = new Library();
        isbn1 = new Isbn("978-5-4461-0914-9");
        isbn2 = new Isbn("978-5-17-090630-2");
        today = LocalDate.of(2026, 3, 1);

        library.addBook(new Book(isbn1, "Effective Java", List.of("Joshua Bloch"), 2019), 1);
        library.addBook(new Book(isbn2, "War and Peace", List.of("Leo Tolstoy"), 2015), 2);

        library.registerReader(new Reader("R-00001", "Student Reader", ReaderCategory.STUDENT));
        library.registerReader(new Reader("R-00002", "Guest Reader", ReaderCategory.GUEST));
    }

    @Test
    void testIssueBookSuccess() {
        Loan loan = library.issueBook("R-00001", isbn1, today);
        assertNotNull(loan);
        assertEquals(1L, loan.id());
        assertTrue(loan.isActive());
        assertEquals(0, library.availableCopies(isbn1));
    }

    @Test
    void testIssueBookReaderNotFound() {
        assertThrows(NotFoundException.class, () -> library.issueBook("R-99999", isbn1, today));
    }

    @Test
    void testIssueBookNotFound() {
        Isbn unknownIsbn = new Isbn("978-0-00-000000-0");
        assertThrows(NotFoundException.class, () -> library.issueBook("R-00001", unknownIsbn, today));
    }

    @Test
    void testIssueBookOverdue() {
        library.issueBook("R-00001", isbn1, today);
        LocalDate lateDate = today.plusDays(15);
        assertThrows(OverdueException.class, () -> library.issueBook("R-00001", isbn2, lateDate));
    }

    @Test
    void testIssueBookLimitExceeded() {
        library.issueBook("R-00002", isbn1, today);
        assertThrows(LimitExceededException.class, () -> library.issueBook("R-00002", isbn2, today));
    }

    @Test
    void testIssueBookUnavailable() {
        library.issueBook("R-00001", isbn1, today);
        assertThrows(BookUnavailableException.class, () -> library.issueBook("R-00002", isbn1, today));
    }

    @Test
    void testImportBooks(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test_books.csv");
        List<String> lines = List.of(
                "isbn;title;authors;year;copies",
                "978-5-4461-0914-9;Java. Эффективное программирование;Джошуа Блох;2019;2",
                "978-5-17-090630-2;Война и мир;Л. Н. Толстой;2015;1",
                "978-5-4461-0960-6;Чистый код;Роберт Мартин;2019;1",
                "978-5-4461-1101-2;Java. Библиотека профессионала;Кей Хорстманн, Гари Корнелл;2022;3",
                "123-456;Книга с неверным ISBN;Автор;2020;1",
                "978-0-306-40615-7;Книга без автора;;2020;1",
                "978-0-262-03384-8;Алгоритмы;Кормен, Лейзерсон, Ривест;год;1",
                "978-0-13-468599-1;Неполная строка;Автор"
        );
        Files.write(csvFile, lines);

        BookCsvImporter importer = new BookCsvImporter();
        ImportResult result = importer.importBooks(csvFile);

        assertEquals(4, result.imported().size());
        assertEquals(4, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Строка 6:"));
        assertTrue(result.errors().get(1).startsWith("Строка 7:"));
        assertTrue(result.errors().get(2).startsWith("Строка 8:"));
        assertTrue(result.errors().get(3).startsWith("Строка 9:"));
    }

    @Test
    void testWriteOverdueReport(@TempDir Path tempDir) throws IOException {
        library.issueBook("R-00002", isbn1, LocalDate.of(2026, 3, 1));
        Path reportFile = tempDir.resolve("overdue.csv");

        ReportWriter writer = new ReportWriter();
        writer.writeOverdueReport(library, LocalDate.of(2026, 3, 10), reportFile);

        List<String> content = Files.readAllLines(reportFile);
        assertEquals(2, content.size());
        assertEquals("cardNumber;fullName;title;dueOn;daysOverdue", content.get(0));
        assertEquals("R-00002;Guest Reader;Effective Java;2026-03-08;2", content.get(1));
    }
}

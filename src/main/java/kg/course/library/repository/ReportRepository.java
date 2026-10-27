package kg.course.library.repository;

import kg.course.library.BookStat;
import kg.course.library.ReaderCategory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Репозиторий отчётов на {@link JdbcClient} (Spring JDBC).
 * <p>
 * Все значения отчётов передаются ТОЛЬКО через именованные параметры
 * ({@code :name}), что обеспечивает защиту от SQL-инъекций (см.
 * {@link #searchBooksByTitle(String)}).
 * </p>
 */
@Repository
public class ReportRepository {

    private final JdbcClient jdbcClient;

    public ReportRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * N самых популярных книг по числу выдач (JOIN + GROUP BY + ORDER BY + LIMIT).
     * Книги без выдач тоже включаются (левое соединение) — как в лабораторной № 3.
     */
    public List<BookStat> topBooks(int n) {
        return jdbcClient.sql("""
                        SELECT b.title   AS title,
                               COUNT(l.id) AS loans
                        FROM books b
                        LEFT JOIN loans l ON l.book_isbn = b.isbn
                        GROUP BY b.isbn, b.title
                        ORDER BY loans DESC, b.title ASC
                        LIMIT :n
                        """)
                .param("n", n)
                .query((rs, rowNum) -> new BookStat(rs.getString("title"), rs.getLong("loans")))
                .list();
    }

    /** Количество выдач по категориям читателей (JOIN + GROUP BY). */
    public Map<ReaderCategory, Long> loansByCategory() {
        return jdbcClient.sql("""
                        SELECT r.category AS category,
                               COUNT(l.id)   AS cnt
                        FROM readers r
                        LEFT JOIN loans l ON l.card_number = r.card_number
                        GROUP BY r.category
                        ORDER BY r.category
                        """)
                .query((rs, rowNum) -> new Object[]{
                        ReaderCategory.valueOf(rs.getString("category")),
                        rs.getLong("cnt")
                })
                .list()
                .stream()
                .collect(LinkedHashMap::new,
                        (map, row) -> map.put((ReaderCategory) row[0], (Long) row[1]),
                        LinkedHashMap::putAll);
    }

    /** Все авторы каталога в алфавитном порядке. */
    public SortedSet<String> allAuthors() {
        return new TreeSet<>(jdbcClient.sql("""
                        SELECT name
                        FROM authors
                        ORDER BY name
                        """)
                .query((rs, rowNum) -> rs.getString("name"))
                .list());
    }

    /**
     * БЕЗОПАСНЫЙ поиск книг по названию через {@link JdbcClient}.
     * <p>
     * Фрагмент передаётся именованным параметром {@code :fragment} — база данных
     * трактует его как литерал, поэтому инъекционные выражения вида
     * {@code ' OR '1'='1} или {@code UNION SELECT ...} не исполняются, а
     * воспринимаются как обычный текст для поиска.
     * </p>
     */
    public List<BookSearchRow> searchBooksByTitle(String fragment) {
        return jdbcClient.sql("""
                        SELECT b.isbn   AS isbn,
                               b.title  AS title,
                               b.year   AS year,
                               b.copies AS copies,
                               COALESCE(STRING_AGG(a.name, ', ' ORDER BY a.name), '') AS authors
                        FROM books b
                        LEFT JOIN book_authors ba ON ba.book_isbn = b.isbn
                        LEFT JOIN authors a       ON a.id = ba.author_id
                        WHERE b.title ILIKE '%' || :fragment || '%'
                        GROUP BY b.isbn, b.title, b.year, b.copies
                        ORDER BY b.title ASC
                        """)
                .param("fragment", fragment)
                .query((rs, rowNum) -> new BookSearchRow(
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getInt("year"),
                        rs.getInt("copies"),
                        rs.getString("authors")))
                .list();
    }

    /** Строка результата безопасного поиска книг по названию. */
    public record BookSearchRow(String isbn, String title, int year, int copies, String authors) {
    }
}

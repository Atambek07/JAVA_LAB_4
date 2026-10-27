package kg.course.library.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.course.library.BookStat;
import kg.course.library.ReaderCategory;
import kg.course.library.repository.ReportRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * REST-контроллер отчётов. Расчёты выполняются SQL-запросами через
 * {@link ReportRepository} (Spring JDBC / {@code JdbcClient}).
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Отчёты", description = "Статистика и отчёты по каталогу")
public class ReportController {

    private final ReportRepository reports;

    public ReportController(ReportRepository reports) {
        this.reports = reports;
    }

    /** N самых популярных книг по числу выдач. */
    @GetMapping("/top")
    @Operation(summary = "Топ книг", description = "n самых популярных книг по числу выдач; значение по умолчанию 5")
    public List<BookStat> topBooks(@RequestParam(value = "n", defaultValue = "5") int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Параметр n должен быть не меньше 1, получено: " + n);
        }
        return reports.topBooks(n);
    }

    /** Количество выдач по категориям читателей. */
    @GetMapping("/by-category")
    @Operation(summary = "Выдачи по категориям")
    public Map<ReaderCategory, Long> loansByCategory() {
        return reports.loansByCategory();
    }

    /** Все авторы каталога в алфавитном порядке. */
    @GetMapping("/authors")
    @Operation(summary = "Все авторы")
    public SortedSet<String> allAuthors() {
        return reports.allAuthors();
    }
}

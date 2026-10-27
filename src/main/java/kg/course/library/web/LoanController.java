package kg.course.library.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.course.library.Isbn;
import kg.course.library.LibraryService;
import kg.course.library.entity.Loan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * REST-контроллер выдач книг.
 */
@RestController
@RequestMapping("/api/loans")
@Tag(name = "Выдачи", description = "Выдача книг читателям и возврат")
public class LoanController {

    private final LibraryService library;
    private final Clock clock;

    public LoanController(LibraryService library, Clock clock) {
        this.library = library;
        this.clock = clock;
    }

    /**
     * Все выдачи; при параметре {@code overdue=true} — только просроченные
     * на текущую дату.
     */
    @GetMapping
    @Operation(summary = "Список выдач", description = "Все выдачи; при параметре overdue=true — только просроченные на текущую дату")
    public List<LoanResponse> findAll(@RequestParam(value = "overdue", defaultValue = "false") boolean overdue) {
        LocalDate today = LocalDate.now(clock);
        List<Loan> loans = overdue ? library.overdueLoans(today) : library.findAllLoans();
        return loans.stream()
                .map(loan -> LoanResponse.from(loan, today))
                .toList();
    }

    /** Выдача по идентификатору. */
    @GetMapping("/{id}")
    @Operation(summary = "Выдача по номеру")
    public LoanResponse findById(@PathVariable long id) {
        LocalDate today = LocalDate.now(clock);
        return LoanResponse.from(library.getLoan(id), today);
    }

    /** Выдача книги читателю. */
    @PostMapping
    @Operation(summary = "Выдать книгу")
    public ResponseEntity<LoanResponse> issue(@Valid @RequestBody IssueRequest request) {
        LocalDate today = LocalDate.now(clock);
        Loan loan = library.issueBook(request.cardNumber(), new Isbn(request.isbn()), today);

        LoanResponse body = LoanResponse.from(loan, today);
        return ResponseEntity
                .created(URI.create("/api/loans/" + loan.getId()))
                .body(body);
    }

    /**
     * Пакетная выдача книг в одной транзакции.
     * Если хотя бы одна книга не может быть выдана — транзакция откатывается,
     * ни одна выдача не сохраняется, возвращается ошибка 404/409.
     */
    @PostMapping("/batch")
    @Operation(summary = "Пакетная выдача",
            description = "Все книги выдаются в одной транзакции: если хотя бы одна не может быть выдана, откат всей пакета")
    public ResponseEntity<List<LoanResponse>> issueBatch(@Valid @RequestBody BatchIssueRequest request) {
        LocalDate today = LocalDate.now(clock);
        List<Loan> loans = library.issueBooksBatch(
                request.cardNumber(),
                request.isbns().stream().map(Isbn::new).toList(),
                today);

        List<LoanResponse> body = loans.stream()
                .map(loan -> LoanResponse.from(loan, today))
                .toList();
        return ResponseEntity.created(URI.create("/api/loans/batch")).body(body);
    }

    /** Возврат книги. */
    @PostMapping("/{id}/return")
    @Operation(summary = "Возврат книги")
    public LoanResponse returnBook(@PathVariable long id) {
        LocalDate today = LocalDate.now(clock);
        Loan returned = library.returnBook(id, today);
        return LoanResponse.from(returned, today);
    }
}

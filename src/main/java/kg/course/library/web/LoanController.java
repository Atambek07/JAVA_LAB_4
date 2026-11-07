package kg.course.library.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.course.library.Isbn;
import kg.course.library.LibraryService;
import kg.course.library.entity.Loan;
import kg.course.library.repository.LoanSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.util.Set;

/**
 * REST-контроллер выдач книг.
 * <p>
 * С лабораторной работы № 5 список выдач поддерживает пагинацию и фильтры
 * ({@code cardNumber}, {@code status}, {@code from}, {@code to}); сортировка
 * по умолчанию — по дате выдачи в порядке убывания. Контракт старого
 * параметра {@code overdue=true} сохранён: он эквивалентен {@code status=OVERDUE}.
 * </p>
 */
@RestController
@RequestMapping("/api/loans")
@Tag(name = "Выдачи", description = "Выдача книг читателям и возврат")
public class LoanController {

    /** Максимальный размер страницы. */
    static final int MAX_PAGE_SIZE = 100;

    /** Допустимые значения статуса выдачи. */
    static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "RETURNED", "OVERDUE");

    private final LibraryService library;
    private final Clock clock;

    public LoanController(LibraryService library, Clock clock) {
        this.library = library;
        this.clock = clock;
    }

    /**
     * Список выдач с пагинацией и фильтрами (Задание 3).
     *
     * @param cardNumber фильтр по номеру билета читателя (необязательно)
     * @param status     статус выдачи: ACTIVE, RETURNED, OVERDUE (необязательно)
     * @param from       нижняя граница диапазона дат выдачи (необязательно)
     * @param to         верхняя граница диапазона дат выдачи (необязательно)
     * @param overdue    устаревший фильтр лабы № 4: эквивалентен status=OVERDUE
     */
    @GetMapping
    @Operation(summary = "Список выдач (пагинация и фильтры)",
            description = "Фильтры: cardNumber, status (ACTIVE/RETURNED/OVERDUE), from, to (диапазон дат выдачи, ISO-8601) + пагинация; сортировка по умолчанию issuedOn,desc")
    public PageResponse<LoanResponse> findAll(
            @RequestParam(value = "cardNumber", required = false) String cardNumber,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "overdue", defaultValue = "false") boolean overdue,
            @PageableDefault(size = 10, sort = "issuedOn", direction = Sort.Direction.DESC) Pageable pageable) {
        validatePageable(pageable);
        validateStatus(status);

        LocalDate today = LocalDate.now(clock);

        // Совместимость с лабой № 4: overdue=true эквивалентен status=OVERDUE
        String effectiveStatus = (status == null || status.isBlank()) && overdue
                ? "OVERDUE"
                : status;

        Page<Loan> page = library.searchLoans(
                LoanSpecifications.search(cardNumber, effectiveStatus, from, to, today), pageable);

        return PageResponse.from(page.map(loan -> LoanResponse.from(loan, today)));
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
    public ResponseEntity<java.util.List<LoanResponse>> issueBatch(@Valid @RequestBody BatchIssueRequest request) {
        LocalDate today = LocalDate.now(clock);
        java.util.List<Loan> loans = library.issueBooksBatch(
                request.cardNumber(),
                request.isbns().stream().map(Isbn::new).toList(),
                today);

        java.util.List<LoanResponse> body = loans.stream()
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

    // ------------------------------------------------------------------
    // Валидация параметров пагинации и фильтров
    // ------------------------------------------------------------------

    /** Проверка размера страницы (не более 100). */
    private static void validatePageable(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Размер страницы не может быть больше " + MAX_PAGE_SIZE + ", получено: " + pageable.getPageSize());
        }
    }

    /** Проверка статуса выдачи: допустимы только ACTIVE, RETURNED, OVERDUE. */
    private static void validateStatus(String status) {
        if (status != null && !status.isBlank() && !ALLOWED_STATUSES.contains(status.trim().toUpperCase())) {
            throw new IllegalArgumentException(
                    "Недопустимый статус выдачи: " + status + ". Допустимые значения: " + ALLOWED_STATUSES);
        }
    }
}

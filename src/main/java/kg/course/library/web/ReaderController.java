package kg.course.library.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.course.library.LibraryService;
import kg.course.library.entity.Reader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * REST-контроллер читателей.
 */
@RestController
@RequestMapping("/api/readers")
@Tag(name = "Читатели", description = "Регистрация читателей и их активные выдачи")
public class ReaderController {

    private final LibraryService library;
    private final Clock clock;

    public ReaderController(LibraryService library, Clock clock) {
        this.library = library;
        this.clock = clock;
    }

    /** Список всех читателей. */
    @GetMapping
    @Operation(summary = "Список читателей")
    public List<ReaderResponse> findAll() {
        return library.findAllReaders().stream()
                .map(ReaderResponse::from)
                .toList();
    }

    /** Читатель по номеру билета. */
    @GetMapping("/{cardNumber}")
    @Operation(summary = "Читатель по номеру билета")
    public ReaderResponse findByCardNumber(@PathVariable String cardNumber) {
        return ReaderResponse.from(library.getReader(cardNumber));
    }

    /**
     * Регистрация читателя. Повторная регистрация билета с тем же номером — 409 Conflict.
     */
    @PostMapping
    @Operation(summary = "Регистрация читателя")
    public ResponseEntity<ReaderResponse> create(@Valid @RequestBody CreateReaderRequest request) {
        if (library.findReader(request.cardNumber()).isPresent()) {
            throw new IllegalStateException(
                    "Читатель с номером билета " + request.cardNumber() + " уже зарегистрирован");
        }

        Reader reader = new Reader(request.cardNumber(), request.fullName(), request.category());
        library.registerReader(reader);

        ReaderResponse body = ReaderResponse.from(reader);
        return ResponseEntity
                .created(URI.create("/api/readers/" + reader.getCardNumber()))
                .body(body);
    }

    /** Активные выдачи читателя. */
    @GetMapping("/{cardNumber}/loans")
    @Operation(summary = "Активные выдачи читателя")
    public List<LoanResponse> activeLoans(@PathVariable String cardNumber) {
        library.getReader(cardNumber);
        LocalDate today = LocalDate.now(clock);
        return library.activeLoans(cardNumber).stream()
                .map(loan -> LoanResponse.from(loan, today))
                .toList();
    }
}

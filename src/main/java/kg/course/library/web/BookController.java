package kg.course.library.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.course.library.Isbn;
import kg.course.library.LibraryException;
import kg.course.library.LibraryService;
import kg.course.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * REST-контроллер каталога книг.
 * <p>
 * С лабораторной работы № 5 списковые эндпоинты поддерживают пагинацию
 * ({@code page}, {@code size}, {@code sort}) и возвращают {@link PageResponse};
 * сортировка разрешена только по полям {@code title} и {@code year}.
 * </p>
 */
@RestController
@RequestMapping("/api/books")
@Tag(name = "Книги", description = "Управление каталогом книг")
public class BookController {

    /** Разрешённые поля сортировки каталога книг. */
    static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "year");

    /** Максимальный размер страницы. */
    static final int MAX_PAGE_SIZE = 100;

    private final LibraryService library;

    public BookController(LibraryService library) {
        this.library = library;
    }

    /**
     * Список книг с пагинацией и сортировкой (Задание 1); при указании
     * параметра {@code q} — поиск по названию.
     */
    @GetMapping
    @Operation(summary = "Список книг (пагинация)",
            description = "Параметры page (с 0), size (по умолчанию 10, не более 100), sort (только title и year); при параметре q — поиск по названию")
    public PageResponse<BookResponse> findAll(@RequestParam(value = "q", required = false) String q,
                                              @PageableDefault(size = 10) Pageable pageable) {
        validatePageable(pageable);

        Page<Book> page = (q == null || q.isBlank())
                ? library.findBooks(pageable)
                : library.searchBooks(
                        kg.course.library.repository.BookSpecifications.hasTitle(q), pageable);

        return PageResponse.from(page.map(book ->
                BookResponse.from(book, library.availableCopies(book.getIsbn()))));
    }

    /**
     * Поиск книг по фильтрам (Задание 2). Все параметры необязательны:
     * отсутствующий параметр не участвует в фильтрации.
     */
    @GetMapping("/search")
    @Operation(summary = "Поиск книг",
            description = "Фильтры: title (фрагмент названия), author (имя автора), yearFrom, yearTo, available (наличие свободных экземпляров) + пагинация")
    public PageResponse<BookResponse> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Boolean available,
            @PageableDefault(size = 10) Pageable pageable) {
        validatePageable(pageable);

        Page<Book> page = library.searchBooks(
                kg.course.library.repository.BookSpecifications.search(
                        title, author, yearFrom, yearTo, available),
                pageable);

        return PageResponse.from(page.map(book ->
                BookResponse.from(book, library.availableCopies(book.getIsbn()))));
    }

    /** Книга по ISBN. */
    @GetMapping("/{isbn}")
    @Operation(summary = "Книга по ISBN")
    public BookResponse findByIsbn(@PathVariable String isbn) {
        Book book = library.getBook(new Isbn(isbn));
        return BookResponse.from(book, library.availableCopies(book.getIsbn()));
    }

    /**
     * Добавление книги. Повторное создание книги с тем же ISBN — 409 Conflict.
     */
    @PostMapping
    @Operation(summary = "Добавить книгу")
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        Isbn isbn = new Isbn(request.isbn());
        if (library.findBook(isbn).isPresent()) {
            throw new IllegalStateException("Книга с ISBN " + isbn.value() + " уже есть в каталоге");
        }

        Book book = new Book(isbn, request.title(), request.authors(), request.year(), request.copies());
        library.addBook(book);

        BookResponse body = BookResponse.from(book, library.availableCopies(isbn));
        return ResponseEntity.created(URI.create("/api/books/" + isbn.value())).body(body);
    }

    /**
     * Изменение книги (Задание 4). Запрос содержит {@code version}, полученную
     * клиентом ранее; при конфликте версий — 409 Conflict.
     */
    @PutMapping("/{isbn}")
    @Operation(summary = "Изменить книгу",
            description = "Обновляет название, год и количество экземпляров; поле version — для оптимистической блокировки")
    public BookResponse update(@PathVariable String isbn, @Valid @RequestBody UpdateBookRequest request) {
        Book book = library.updateBook(
                new Isbn(isbn), request.title(), request.year(), request.copies(), request.version());
        return BookResponse.from(book, library.availableCopies(book.getIsbn()));
    }

    /** Удаление книги; если у книги есть активные выдачи — 409 Conflict. */
    @DeleteMapping("/{isbn}")
    @Operation(summary = "Удалить книгу")
    public ResponseEntity<Void> delete(@PathVariable String isbn) {
        library.removeBook(new Isbn(isbn));
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Валидация параметров пагинации
    // ------------------------------------------------------------------

    /** Проверка размера страницы (не более 100) и полей сортировки (только title и year). */
    private static void validatePageable(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Размер страницы не может быть больше " + MAX_PAGE_SIZE + ", получено: " + pageable.getPageSize());
        }
        List<String> invalidFields = pageable.getSort().stream()
                .map(order -> order.getProperty())
                .filter(property -> !ALLOWED_SORT_FIELDS.contains(property))
                .toList();
        if (!invalidFields.isEmpty()) {
            throw new InvalidSortException(invalidFields, ALLOWED_SORT_FIELDS);
        }
    }
}

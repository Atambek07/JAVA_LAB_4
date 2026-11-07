package kg.course.library.web;

import kg.course.library.BookUnavailableException;
import kg.course.library.Isbn;
import kg.course.library.LibraryService;
import kg.course.library.NotFoundException;
import kg.course.library.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты веб-слоя контроллера книг (Задание 6).
 * <p>
 * Используется срез {@code @WebMvcTest} (только MVC-бины): сервисный слой
 * заменяется заглушкой через {@code @MockitoBean} (Spring Boot 4: {@code @MockBean}
 * устарел). Проверяются коды ответов, формат ProblemDetail (RFC 9457) и
 * свойство {@code errors} при ошибках валидации.
 * </p>
 */
@WebMvcTest(BookController.class)
class BookControllerTest {

    private static final String ISBN = "978-0-13-468599-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryService library;

    @BeforeEach
    void setUp() {
        // По умолчанию считаем, что книги нет; каждый тест настраивает свои заглушки
        when(library.findBook(any(Isbn.class))).thenReturn(Optional.empty());
    }

    // ------------------------------------------------------------------
    // Задание 1: пагинация и сортировка
    // ------------------------------------------------------------------

    @Test
    void findAll_returnsPageResponseShape() throws Exception {
        Book book = new Book(new Isbn(ISBN), "Effective Java", List.of("Джошуа Блох"), 2018, 2);
        var page = new PageImpl<>(List.of(book), PageRequest.of(0, 10, Sort.by("title")), 25);

        when(library.findBooks(any())).thenReturn(page);
        when(library.availableCopies(book.getIsbn())).thenReturn(1);

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].isbn").value("9780134685991"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void findAll_oversizedPage_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/books").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", containsString("больше 100")));
    }

    @Test
    void findAll_invalidSortField_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/books").param("sort", "author,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", containsString("title")));

        // Сортировка проверяется до вызова сервиса
        verify(library, never()).findBooks(any());
    }

    // ------------------------------------------------------------------
    // Задание 4: PUT с оптимистической блокировкой
    // ------------------------------------------------------------------

    @Test
    void update_returnsUpdatedBookWithNewVersion() throws Exception {
        Book updated = new Book(new Isbn(ISBN), "Effective Java, 3rd Edition", List.of("Джошуа Блох"), 2018, 3);
        updated.setVersion(1);

        when(library.updateBook(any(Isbn.class), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(updated);
        when(library.availableCopies(updated.getIsbn())).thenReturn(3);

        mockMvc.perform(put("/api/books/" + ISBN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Effective Java, 3rd Edition", "year": 2018, "copies": 3, "version": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Effective Java, 3rd Edition"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void update_staleVersion_returns409ProblemDetail() throws Exception {
        when(library.updateBook(any(Isbn.class), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Book.class, "9780134685991"));

        mockMvc.perform(put("/api/books/" + ISBN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Effective Java", "year": 2018, "copies": 2, "version": 0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void update_copiesBelowActiveLoans_returns409() throws Exception {
        when(library.updateBook(any(Isbn.class), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BookUnavailableException("Нельзя уменьшить количество экземпляров"));

        mockMvc.perform(put("/api/books/" + ISBN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Effective Java", "year": 2018, "copies": 1, "version": 0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void update_missingVersion_returns400WithErrors() throws Exception {
        mockMvc.perform(put("/api/books/" + ISBN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Effective Java", "year": 2018, "copies": 2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("version")));
    }

    // ------------------------------------------------------------------
    // Проверка контрактов прошлых лабораторных
    // ------------------------------------------------------------------

    @Test
    void create_returns201AndLocation() throws Exception {
        Book book = new Book(new Isbn(ISBN), "Effective Java", List.of("Джошуа Блох"), 2018, 2);
        when(library.addBook(any(Book.class))).thenReturn(book);
        when(library.availableCopies(book.getIsbn())).thenReturn(2);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "978-0-13-468599-1", "title": "Effective Java",
                                 "authors": ["Джошуа Блох"], "year": 2018, "copies": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/books/9780134685991"))
                .andExpect(jsonPath("$.isbn").value("9780134685991"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void create_blankTitle_returns400WithErrors() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "978-0-13-468599-1", "title": " ",
                                 "authors": ["Джошуа Блох"], "year": 2018, "copies": 2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")));
    }

    @Test
    void findByIsbn_unknownBook_returns404ProblemJson() throws Exception {
        when(library.getBook(any(Isbn.class)))
                .thenThrow(new NotFoundException("Книга с ISBN 9780000000000 не найдена в каталоге"));

        mockMvc.perform(get("/api/books/978-0-00-000000-0"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }
}

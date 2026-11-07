package kg.course.library;

import kg.course.library.entity.Book;
import kg.course.library.entity.Loan;
import kg.course.library.entity.Reader;
import kg.course.library.repository.AuthorRepository;
import kg.course.library.repository.BookRepository;
import kg.course.library.repository.LoanRepository;
import kg.course.library.repository.ReaderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Модульные тесты сервисного слоя {@link LibraryService} (Задание 5).
 * <p>
 * Репозитории заменяются заглушками ({@code @Mock}), сервис создаётся через
 * {@code @InjectMocks}. Текущая дата задана фиксированным {@code Clock.fixed}
 * (аналог {@code TestClockConfig} из лабораторной работы № 4): 2026-03-20.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    /** Фиксированная «сегодняшняя» дата для всех тестов. */
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 20);

    /** Фиксированные часы: Clock.fixed из лабы № 4. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private ReaderRepository readerRepository;
    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private LibraryService service;

    // ------------------------------------------------------------------
    // Вспомогательные фабрики тестовых данных
    // ------------------------------------------------------------------

    private Reader student() {
        return new Reader("R-00001", "Студент", ReaderCategory.STUDENT);
    }

    private Reader guest() {
        return new Reader("R-00002", "Гость", ReaderCategory.GUEST);
    }

    private Book book(String isbn, int copies) {
        return new Book(new Isbn(isbn), "Тестовая книга", List.of("Автор"), 2020, copies);
    }

    // ------------------------------------------------------------------
    // Задание 5. issueBook: успешная выдача + 5 бизнес-правил
    // ------------------------------------------------------------------

    @Test
    void issueBook_success_savesLoanOnce() {
        Reader reader = student();
        Book book = book("978-0-13-468599-1", 2);

        when(readerRepository.findById("R-00001")).thenReturn(Optional.of(reader));
        when(bookRepository.findByIsbn(book.getIsbn())).thenReturn(Optional.of(book));
        // Правило 3: нет просрочек
        when(loanRepository.existsByReaderCardNumberAndReturnedOnIsNullAndDueOnBefore("R-00001", TODAY))
                .thenReturn(false);
        // Правило 4: лимит категории STUDENT = 3
        when(loanRepository.countByReaderCardNumberAndReturnedOnIsNull("R-00001")).thenReturn(0L);
        // Правило 5: есть свободные экземпляры
        when(loanRepository.countByBookIsbnAndReturnedOnIsNull(book.getIsbn())).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan loan = service.issueBook("R-00001", book.getIsbn(), LocalDate.now(FIXED_CLOCK));

        // Срок возврата: STUDENT = 14 дней
        assertThat(loan.getDueOn()).isEqualTo(TODAY.plusDays(14));
        assertThat(loan.isActive()).isTrue();
        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    void issueBook_readerNotFound_throwsAndNeverSaves() {
        when(readerRepository.findById("R-99999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueBook("R-99999",
                new Isbn("978-0-13-468599-1"), TODAY))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("R-99999");

        verify(loanRepository, never()).save(any());
    }

    @Test
    void issueBook_bookNotFound_throwsAndNeverSaves() {
        Reader reader = student();
        Isbn unknown = new Isbn("978-0-00-000000-0");

        when(readerRepository.findById("R-00001")).thenReturn(Optional.of(reader));
        when(bookRepository.findByIsbn(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueBook("R-00001", unknown, TODAY))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("9780000000000");

        verify(loanRepository, never()).save(any());
    }

    @Test
    void issueBook_readerHasOverdueLoan_throwsOverdueExceptionAndNeverSaves() {
        Reader reader = student();
        Book book = book("978-0-13-468599-1", 2);

        when(readerRepository.findById("R-00001")).thenReturn(Optional.of(reader));
        when(bookRepository.findByIsbn(book.getIsbn())).thenReturn(Optional.of(book));
        when(loanRepository.existsByReaderCardNumberAndReturnedOnIsNullAndDueOnBefore("R-00001", TODAY))
                .thenReturn(true);

        assertThatThrownBy(() -> service.issueBook("R-00001", book.getIsbn(), TODAY))
                .isInstanceOf(OverdueException.class)
                .hasMessageContaining("просроченные");

        verify(loanRepository, never()).save(any());
    }

    @Test
    void issueBook_categoryLimitExceeded_throwsLimitExceededExceptionAndNeverSaves() {
        Reader reader = student();
        Book book = book("978-0-13-468599-1", 2);

        when(readerRepository.findById("R-00001")).thenReturn(Optional.of(reader));
        when(bookRepository.findByIsbn(book.getIsbn())).thenReturn(Optional.of(book));
        when(loanRepository.existsByReaderCardNumberAndReturnedOnIsNullAndDueOnBefore("R-00001", TODAY))
                .thenReturn(false);
        // Лимит STUDENT = 3: уже на руках 3 книги
        when(loanRepository.countByReaderCardNumberAndReturnedOnIsNull("R-00001")).thenReturn(3L);

        assertThatThrownBy(() -> service.issueBook("R-00001", book.getIsbn(), TODAY))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("лимит");

        verify(loanRepository, never()).save(any());
    }

    @Test
    void issueBook_noFreeCopies_throwsBookUnavailableExceptionAndNeverSaves() {
        Reader reader = student();
        Book book = book("978-0-13-468599-1", 1);

        when(readerRepository.findById("R-00001")).thenReturn(Optional.of(reader));
        when(bookRepository.findByIsbn(book.getIsbn())).thenReturn(Optional.of(book));
        when(loanRepository.existsByReaderCardNumberAndReturnedOnIsNullAndDueOnBefore("R-00001", TODAY))
                .thenReturn(false);
        when(loanRepository.countByReaderCardNumberAndReturnedOnIsNull("R-00001")).thenReturn(0L);
        // Единственный экземпляр уже на руках
        when(loanRepository.countByBookIsbnAndReturnedOnIsNull(book.getIsbn())).thenReturn(1L);

        assertThatThrownBy(() -> service.issueBook("R-00001", book.getIsbn(), TODAY))
                .isInstanceOf(BookUnavailableException.class)
                .hasMessageContaining("свободных экземпляров");

        verify(loanRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Задание 4. updateBook: оптимистическая блокировка и лимит экземпляров
    // ------------------------------------------------------------------

    @Test
    void updateBook_success_updatesFieldsAndSaves() {
        Book book = book("978-0-13-468599-1", 3);
        book.setVersion(0); // версия, полученная клиентом ранее

        when(bookRepository.findByIsbn(book.getIsbn())).thenReturn(Optional.of(book));
        when(loanRepository.countByBookIsbnAndReturnedOnIsNull(book.getIsbn())).thenReturn(1L);
        when(bookRepository.save(book)).thenAnswer(inv -> inv.getArgument(0));

        Book updated = service.updateBook(book.getIsbn(), "Новое название", 2021, 5, 0);

        assertThat(updated.getTitle()).isEqualTo("Новое название");
        assertThat(updated.getYear()).isEqualTo(2021);
        assertThat(updated.getCopies()).isEqualTo(5);
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void updateBook_staleVersion_throwsOptimisticLockFailure() {
        Book book = book("978-0-13-468599-1", 3);
        book.setVersion(1); // в базе версия 1, клиент прислал устаревшую 0

        when(bookRepository.findByIsbn(book.getIsbn())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> service.updateBook(book.getIsbn(), "Новое", 2021, 2, 0))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateBook_copiesBelowActiveLoans_throwsBookUnavailableException() {
        Book book = book("978-0-13-468599-1", 3);
        book.setVersion(0);

        when(bookRepository.findByIsbn(book.getIsbn())).thenReturn(Optional.of(book));
        // 2 активные выдачи — нельзя уменьшить количество до 1
        when(loanRepository.countByBookIsbnAndReturnedOnIsNull(book.getIsbn())).thenReturn(2L);

        assertThatThrownBy(() -> service.updateBook(book.getIsbn(), "Новое", 2021, 1, 0))
                .isInstanceOf(BookUnavailableException.class)
                .hasMessageContaining("Нельзя уменьшить количество экземпляров");

        verify(bookRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Задание 1/2: пагинация и поиск книг (делегирование в репозиторий)
    // ------------------------------------------------------------------

    @Test
    void findBooks_delegatesToRepositoryWithPageable() {
        Pageable pageable = PageRequest.of(0, 10);
        Book book = book("978-0-13-468599-1", 2);
        when(bookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(book), pageable, 1));

        var result = service.findBooks(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(bookRepository).findAll(pageable);
    }

    @Test
    void searchBooks_delegatesSpecificationToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(bookRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = service.searchBooks(
                kg.course.library.repository.BookSpecifications.hasTitle("java"), pageable);

        assertThat(result.getContent()).isEmpty();
        ArgumentCaptor<Specification<Book>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(bookRepository).findAll(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    void searchLoans_delegatesSpecificationToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(loanRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = service.searchLoans(
                kg.course.library.repository.LoanSpecifications.hasCardNumber("R-00001"), pageable);

        assertThat(result.getContent()).isEmpty();
        verify(loanRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ------------------------------------------------------------------
    // addBook: разрешение существующего автора (без дубликатов)
    // ------------------------------------------------------------------

    @Test
    void addBook_reusesExistingAuthor() {
        Book book = book("978-0-13-468599-1", 2);
        kg.course.library.entity.Author existing = new kg.course.library.entity.Author("Автор");

        when(authorRepository.findByName("Автор")).thenReturn(Optional.of(existing));
        when(bookRepository.save(book)).thenAnswer(inv -> inv.getArgument(0));

        Book saved = service.addBook(book);

        assertThat(saved.getAuthors()).containsExactly(existing);
        verify(authorRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Вспомогательная проверка: отсутствие лишних взаимодействий
    // ------------------------------------------------------------------

    @Test
    void issueBook_whenReaderLookupFails_doesNotQueryBooks() {
        when(readerRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueBook("R-00000",
                new Isbn("978-0-13-468599-1"), TODAY))
                .isInstanceOf(NotFoundException.class);

        verify(bookRepository, never()).findByIsbn(any());
        verify(loanRepository, never()).save(any());
        verify(loanRepository, never()).existsByReaderCardNumberAndReturnedOnIsNullAndDueOnBefore(anyString(), any());
        verify(loanRepository, never()).countByReaderCardNumberAndReturnedOnIsNull(anyString());
        verify(loanRepository, never()).countByBookIsbnAndReturnedOnIsNull(any());
    }
}

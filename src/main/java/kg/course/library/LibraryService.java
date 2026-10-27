package kg.course.library;

import kg.course.library.entity.Author;
import kg.course.library.entity.Book;
import kg.course.library.entity.Loan;
import kg.course.library.entity.Reader;
import kg.course.library.repository.AuthorRepository;
import kg.course.library.repository.BookRepository;
import kg.course.library.repository.LoanRepository;
import kg.course.library.repository.ReaderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сервисный слой «Библиотека» — заменяет in-memory класс {@code Library}
 * лабораторной работы № 3. Вся бизнес-логика выполняется поверх PostgreSQL
 * (Spring Data JPA).
 * <p>
 * Методы, изменяющие данные, помечены {@code @Transactional}; методы чтения —
 * {@code @Transactional(readOnly = true)}. Пять проверок при выдаче книги
 * сохраняются в порядке лабораторной работы № 2:
 * <ol>
 *   <li>читатель существует;</li>
 *   <li>книга существует;</li>
 *   <li>нет просроченных выдач;</li>
 *   <li>лимит категории не превышен;</li>
 *   <li>есть свободные экземпляры.</li>
 * </ol>
 * </p>
 */
@Service
public class LibraryService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final ReaderRepository readerRepository;
    private final LoanRepository loanRepository;

    public LibraryService(BookRepository bookRepository,
                          AuthorRepository authorRepository,
                          ReaderRepository readerRepository,
                          LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.readerRepository = readerRepository;
        this.loanRepository = loanRepository;
    }

    // ------------------------------------------------------------------
    // Книги
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Book> findAllBooks() {
        return bookRepository.findAllByOrderByTitleAsc();
    }

    @Transactional(readOnly = true)
    public List<Book> searchByTitle(String fragment) {
        return bookRepository.findByTitleContainingIgnoreCaseOrderByTitleAsc(fragment);
    }

    @Transactional(readOnly = true)
    public Optional<Book> findBook(Isbn isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    @Transactional(readOnly = true)
    public Book getBook(Isbn isbn) {
        return findBook(isbn).orElseThrow(() ->
                new NotFoundException("Книга с ISBN " + (isbn != null ? isbn.value() : "null") + " не найдена в каталоге"));
    }

    @Transactional(readOnly = true)
    public int totalCopies(Isbn isbn) {
        return getBook(isbn).getCopies();
    }

    @Transactional(readOnly = true)
    public int availableCopies(Isbn isbn) {
        return totalCopies(isbn) - (int) loanRepository.countByBookIsbnAndReturnedOnIsNull(isbn);
    }

    /**
     * Добавление книги в одной транзакции. Автор, уже существующий в базе,
     * не создаётся повторно: сначала выполняется поиск по имени.
     */
    @Transactional
    public Book addBook(Book book) {
        Set<Author> resolvedAuthors = new LinkedHashSet<>();
        for (Author author : book.getAuthors()) {
            Author resolved = authorRepository.findByName(author.getName())
                    .orElseGet(() -> authorRepository.save(new Author(author.getName())));
            resolvedAuthors.add(resolved);
        }
        book.setAuthors(resolvedAuthors);
        return bookRepository.save(book);
    }

    /**
     * Удаление книги. При активных выдачах — {@link IllegalStateException} (409);
     * иначе удаляются прошлые (возвращённые) выдачи и сама книга.
     */
    @Transactional
    public void removeBook(Isbn isbn) {
        Book book = getBook(isbn);

        if (loanRepository.existsByBookIsbnAndReturnedOnIsNull(isbn)) {
            throw new IllegalStateException(
                    "Нельзя удалить книгу \"" + book.getTitle() + "\" (ISBN " + isbn.value() + "): есть активные выдачи");
        }

        loanRepository.deleteByBookIsbn(isbn);
        bookRepository.delete(book);
    }

    // ------------------------------------------------------------------
    // Читатели
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Reader> findAllReaders() {
        return readerRepository.findAll(Sort.by("cardNumber"));
    }

    @Transactional(readOnly = true)
    public Optional<Reader> findReader(String cardNumber) {
        return readerRepository.findById(cardNumber);
    }

    @Transactional(readOnly = true)
    public Reader getReader(String cardNumber) {
        return findReader(cardNumber).orElseThrow(() ->
                new NotFoundException("Читатель с номером билета " + cardNumber + " не зарегистрирован"));
    }

    @Transactional
    public Reader registerReader(Reader reader) {
        return readerRepository.save(reader);
    }

    // ------------------------------------------------------------------
    // Выдачи
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Loan> findAllLoans() {
        return loanRepository.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public Loan getLoan(long id) {
        return loanRepository.findLoanById(id).orElseThrow(() ->
                new NotFoundException("Выдача с номером " + id + " не найдена"));
    }

    @Transactional(readOnly = true)
    public List<Loan> overdueLoans(LocalDate today) {
        return loanRepository.findByReturnedOnIsNullAndDueOnBeforeOrderByDueOnAsc(today);
    }

    @Transactional(readOnly = true)
    public List<Loan> activeLoans(String cardNumber) {
        return loanRepository.findByReaderCardNumberAndReturnedOnIsNull(cardNumber);
    }

    /** Выдача книги читателю (пять проверок, одна транзакция). */
    @Transactional
    public Loan issueBook(String cardNumber, Isbn isbn, LocalDate date) {
        Reader reader = getReader(cardNumber);
        Book book = getBook(isbn);
        return issueValidated(reader, book, date);
    }

    /**
     * Пакетная выдача книг: все выдачи выполняются в ОДНОЙ транзакции.
     * Если выдача хотя бы одной книги невозможна, ни одна не сохраняется
     * (транзакция откатывается полностью).
     */
    @Transactional
    public List<Loan> issueBooksBatch(String cardNumber, List<Isbn> isbns, LocalDate date) {
        Reader reader = getReader(cardNumber);
        List<Loan> issued = new ArrayList<>();
        for (Isbn isbn : isbns) {
            Book book = getBook(isbn);
            issued.add(issueValidated(reader, book, date));
        }
        return issued;
    }

    /** Возврат книги читателем. */
    @Transactional
    public Loan returnBook(long id, LocalDate date) {
        Loan loan = getLoan(id);
        return loanRepository.save(loan.markReturned(date));
    }

    // ------------------------------------------------------------------
    // Внутренняя логика: пять проверок
    // ------------------------------------------------------------------

    /**
     * Единая точка выполнения пяти бизнес-проверок при выдаче. Работает как
     * внутри обычной выдачи, так и внутри пакетной (loop в одной транзакции).
     */
    private Loan issueValidated(Reader reader, Book book, LocalDate date) {
        String cardNumber = reader.getCardNumber();

        // Проверка 3: нет просроченных выдач
        if (loanRepository.existsByReaderCardNumberAndReturnedOnIsNullAndDueOnBefore(cardNumber, date)) {
            throw new OverdueException("У читателя " + cardNumber + " есть просроченные выдачи на дату " + date);
        }

        // Проверка 4: лимит категории
        if (loanRepository.countByReaderCardNumberAndReturnedOnIsNull(cardNumber) >= reader.getCategory().maxBooks()) {
            throw new LimitExceededException(
                    "Превышен лимит книг на руках (" + reader.getCategory().maxBooks() + ") для читателя " + cardNumber);
        }

        // Проверка 5: есть свободные экземпляры
        if (loanRepository.countByBookIsbnAndReturnedOnIsNull(book.getIsbn()) >= book.getCopies()) {
            throw new BookUnavailableException("Нет свободных экземпляров книги " + book.getTitle() + " ");
        }

        LocalDate dueOn = reader.getCategory().dueDate(date);
        return loanRepository.save(new Loan(book, reader, date, dueOn, null));
    }
}

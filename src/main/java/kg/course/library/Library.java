package kg.course.library;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Library {
    private final Map<Isbn, Book> books = new LinkedHashMap<>();
    private final Map<Isbn, Integer> copies = new LinkedHashMap<>();
    private final Map<String, Reader> readers = new LinkedHashMap<>();
    private final List<Loan> loans = new ArrayList<>();
    private long nextLoanId = 1;

    public void addBook(Book book, int count) {
        if (book == null) {
            throw new IllegalArgumentException("Книга не может быть null");
        }
        if (count < 1) {
            throw new IllegalArgumentException("Количество экземпляров должно быть не меньше 1");
        }
        books.putIfAbsent(book.isbn(), book);
        copies.merge(book.isbn(), count, Integer::sum);
    }

    public Optional<Book> findBook(Isbn isbn) {
        return Optional.ofNullable(books.get(isbn));
    }

    public Book getBook(Isbn isbn) {
        return findBook(isbn).orElseThrow(() ->
                new NotFoundException("Книга с ISBN " + (isbn != null ? isbn.value() : "null") + " не найдена в каталоге"));
    }

    public int totalCopies(Isbn isbn) {
        getBook(isbn);
        return copies.getOrDefault(isbn, 0);
    }

    public int availableCopies(Isbn isbn) {
        int total = totalCopies(isbn);
        long onHand = loans.stream()
                .filter(loan -> loan.isbn().equals(isbn) && loan.isActive())
                .count();
        return total - (int) onHand;
    }

    public void removeBook(Isbn isbn) {
        Book book = getBook(isbn);

        boolean hasActiveLoans = loans.stream()
                .anyMatch(loan -> loan.isbn().equals(isbn) && loan.isActive());
        if (hasActiveLoans) {
            throw new IllegalStateException(
                    "Нельзя удалить книгу \"" + book.title() + "\" (ISBN " + isbn.value() + "): есть активные выдачи");
        }

        books.remove(isbn);
        copies.remove(isbn);
    }

    public void registerReader(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Читатель не может быть null");
        }
        if (readers.containsKey(reader.cardNumber())) {
            throw new IllegalArgumentException("Читатель с номером билета " + reader.cardNumber() + " уже зарегистрирован");
        }
        readers.put(reader.cardNumber(), reader);
    }

    public Optional<Reader> findReader(String cardNumber) {
        return Optional.ofNullable(readers.get(cardNumber));
    }

    public Reader getReader(String cardNumber) {
        return findReader(cardNumber).orElseThrow(() ->
                new NotFoundException("Читатель с номером билета " + cardNumber + " не зарегистрирован"));
    }

    public List<Book> findAllBooks() {
        return List.copyOf(books.values());
    }

    public List<Reader> findAllReaders() {
        return List.copyOf(readers.values());
    }

    public List<Loan> findAllLoans() {
        return List.copyOf(loans);
    }

    public Loan issueBook(String cardNumber, Isbn isbn, LocalDate date) {
        Reader reader = getReader(cardNumber);
        Book book = getBook(isbn);

        boolean hasOverdue = loans.stream()
                .anyMatch(loan -> loan.cardNumber().equals(cardNumber) && loan.isOverdue(date));
        if (hasOverdue) {
            throw new OverdueException("У читателя " + cardNumber + " есть просроченные выдачи на дату " + date);
        }

        if (activeLoans(cardNumber).size() >= reader.category().maxBooks()) {
            throw new LimitExceededException("Превышен лимит книг на руках (" + reader.category().maxBooks() + ") для читателя " + cardNumber);
        }

        if (availableCopies(book.isbn()) <= 0) {
            throw new BookUnavailableException("Нет свободных экземпляров книги " + book.title() + " ");
        }

        LocalDate dueOn = reader.category().dueDate(date);
        Loan loan = new Loan(nextLoanId++, book.isbn(), cardNumber, date, dueOn, null);
        loans.add(loan);
        return loan;
    }

    public Loan returnBook(long loanId, LocalDate date) {
        for (int i = 0; i < loans.size(); i++) {
            Loan loan = loans.get(i);
            if (loan.id() == loanId) {
                Loan returnedLoan = loan.returned(date);
                loans.set(i, returnedLoan);
                return returnedLoan;
            }
        }
        throw new NotFoundException("Выдача с номером " + loanId + " не найдена");
    }

    public List<Loan> activeLoans(String cardNumber) {
        return loans.stream()
                .filter(loan -> loan.cardNumber().equals(cardNumber) && loan.isActive())
                .toList();
    }
}

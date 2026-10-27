package kg.course.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * JPA-сущность «Выдача книги».
 * <p>
 * Связи «многие-к-одному» с {@link Book} и {@link Reader} выполнены ленивыми
 * ({@code FetchType.LAZY}). Для чтения используется {@code @EntityGraph},
 * который подгружает обе связи одним SQL-запросом (исправление проблема N+1).
 * </p>
 * <p>
 * Бизнес-правила выдачи перенесены внутрь сущности: {@link #isActive()},
 * {@link #isOverdue(LocalDate)} и {@link #markReturned(LocalDate)}.
 * </p>
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_isbn", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_number", nullable = false)
    private Reader reader;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @Column(name = "returned_on")
    private LocalDate returnedOn;

    /** Конструктор без параметров для JPA (protected). */
    protected Loan() {
    }

    /** Открытый конструктор с проверкой аргументов. */
    public Loan(Book book, Reader reader, LocalDate issuedOn, LocalDate dueOn, LocalDate returnedOn) {
        if (book == null || reader == null || issuedOn == null || dueOn == null) {
            throw new IllegalArgumentException("Обязательные поля выдачи не могут быть null");
        }
        if (dueOn.isBefore(issuedOn)) {
            throw new IllegalArgumentException("Срок возврата не может быть раньше даты выдачи");
        }
        if (returnedOn != null && returnedOn.isBefore(issuedOn)) {
            throw new IllegalArgumentException("Фактическая дата возврата не может быть раньше даты выдачи");
        }
        this.book = book;
        this.reader = reader;
        this.issuedOn = issuedOn;
        this.dueOn = dueOn;
        this.returnedOn = returnedOn;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Reader getReader() {
        return reader;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    /** Выдача активна, если книга ещё не возвращена. */
    public boolean isActive() {
        return returnedOn == null;
    }

    /** Выдача просрочена, если активна и срок возврата уже прошёл. */
    public boolean isOverdue(LocalDate today) {
        return isActive() && today.isAfter(dueOn);
    }

    /**
     * Логика возврата книги: помечает выдачу возвращённой.
     * Повторный возврат запрещён.
     */
    public Loan markReturned(LocalDate date) {
        if (!isActive()) {
            throw new IllegalStateException("Книга по выдаче №" + id + " уже возвращена");
        }
        this.returnedOn = date;
        return this;
    }

    /** {@code equals}/{@code hashCode} — только по первичному ключу. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Loan loan = (Loan) o;
        return id != null && Objects.equals(id, loan.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Loan{id=" + id + ", isbn=" + (book != null ? book.getIsbn() : "?") + ", card="
                + (reader != null ? reader.getCardNumber() : "?") + ", issuedOn=" + issuedOn + ", dueOn=" + dueOn + "}";
    }
}

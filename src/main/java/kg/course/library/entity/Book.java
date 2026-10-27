package kg.course.library.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import kg.course.library.Isbn;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * JPA-сущность «Книга».
 * <p>
 * Первичный ключ — ISBN (колонка {@code varchar(13)}). Доменный тип
 * {@link Isbn} встраивается в сущность через {@code @EmbeddedId}
 * (в колонке {@code isbn} сохраняется нормализованное значение).
 * Связь с авторами — {@code @ManyToMany} через таблицу {@code book_authors}.
 * </p>
 */
@Entity
@Table(name = "books")
public class Book {

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "isbn", length = 13, nullable = false))
    private Isbn isbn;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "copies", nullable = false)
    private int copies;

    @ManyToMany
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_isbn"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<Author> authors = new LinkedHashSet<>();

    /** Конструктор без параметров для JPA (protected). */
    protected Book() {
    }

    /**
     * Открытый конструктор с проверкой аргументов.
     *
     * @param isbn        нормализуемый ISBN
     * @param title       название книги
     * @param authorNames имена авторов (создаются временные {@link Author})
     * @param year        год издания
     * @param copies      количество экземпляров
     */
    public Book(Isbn isbn, String title, List<String> authorNames, int year, int copies) {
        if (isbn == null) {
            throw new IllegalArgumentException("ISBN не задан");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Название книги не может быть пустым");
        }
        if (authorNames == null || authorNames.isEmpty()) {
            throw new IllegalArgumentException("Список авторов не может быть пустым");
        }
        if (year < 1450 || year > LocalDate.now().getYear()) {
            throw new IllegalArgumentException("Год издания вне допустимого диапазона: " + year);
        }
        if (copies < 1) {
            throw new IllegalArgumentException("Количество экземпляров должно быть не меньше 1");
        }
        this.isbn = isbn;
        this.title = title;
        this.year = year;
        this.copies = copies;
        Set<Author> initial = new LinkedHashSet<>();
        for (String name : authorNames) {
            initial.add(new Author(name));
        }
        this.authors = initial;
    }

    public Isbn getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Author> authors) {
        this.authors = authors;
    }

    public void addAuthor(Author author) {
        this.authors.add(author);
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
        Book book = (Book) o;
        return isbn != null && Objects.equals(isbn, book.isbn);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Book{isbn=" + isbn + ", title='" + title + "', year=" + year + ", copies=" + copies + ", authors=" + authors + "}";
    }
}

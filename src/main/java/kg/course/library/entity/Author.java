package kg.course.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * JPA-сущность «Автор».
 * <p>
 * Первичный ключ {@code id} генерируется базой (identity). Поле {@code name}
 * уникально: при добавлении книги существующий автор не создаётся повторно.
 * </p>
 */
@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "authors", fetch = FetchType.LAZY)
    private Set<Book> books = new LinkedHashSet<>();

    /** Конструктор без параметров для JPA (protected). */
    protected Author() {
    }

    /** Открытый конструктор с проверкой аргументов. */
    public Author(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя автора не может быть пустым");
        }
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Book> getBooks() {
        return books;
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
        Author author = (Author) o;
        return id != null && Objects.equals(id, author.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Author{id=" + id + ", name='" + name + "'}";
    }
}

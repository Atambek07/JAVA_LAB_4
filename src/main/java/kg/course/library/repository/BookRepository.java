package kg.course.library.repository;

import kg.course.library.Isbn;
import kg.course.library.entity.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий книг. {@code @EntityGraph(attributePaths = "authors")}
 * подтягивает авторов одним запросом (без N+1).
 */
public interface BookRepository extends JpaRepository<Book, Isbn> {

    /** Все книги, отсортированные по названию, с авторами. */
    @EntityGraph(attributePaths = "authors")
    List<Book> findAllByOrderByTitleAsc();

    /** Книга по ISBN с авторами. */
    @EntityGraph(attributePaths = "authors")
    Optional<Book> findByIsbn(Isbn isbn);

    /**
     * Поиск по фрагменту названия без учёта регистра
     * (запрос строится Spring Data по имени метода), с сортировкой по названию.
     */
    @EntityGraph(attributePaths = "authors")
    List<Book> findByTitleContainingIgnoreCaseOrderByTitleAsc(String fragment);
}

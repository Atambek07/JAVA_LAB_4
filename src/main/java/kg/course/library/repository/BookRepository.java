package kg.course.library.repository;

import kg.course.library.Isbn;
import kg.course.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий книг. {@code @EntityGraph(attributePaths = "authors")}
 * подтягивает авторов одним запросом (без N+1).
 * <p>
 * Начиная с лабораторной работы № 5 также наследует
 * {@link JpaSpecificationExecutor} — динамическая фильтрация
 * (см. {@link BookSpecifications}).
 * </p>
 */
public interface BookRepository extends JpaRepository<Book, Isbn>, JpaSpecificationExecutor<Book> {

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

    /**
     * Постраничная выборка всех книг с авторами (Задание 1).
     * Переопределение стандартного {@code findAll(Pageable)} — @EntityGraph
     * устраняет проблему N+1 при загрузке авторов.
     */
    @Override
    @EntityGraph(attributePaths = "authors")
    Page<Book> findAll(Pageable pageable);

    /**
     * Постраничная выборка книг по спецификации с авторами (Задание 2).
     * С Spring Data JPA 3.5+ методы JpaSpecificationExecutor можно
     * переопределять и снабжать @EntityGraph.
     */
    @Override
    @EntityGraph(attributePaths = "authors")
    Page<Book> findAll(Specification<Book> spec, Pageable pageable);
}

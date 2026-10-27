package kg.course.library.repository;

import kg.course.library.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий авторов. Поиск по имени используется при добавлении книги,
 * чтобы не создавать уже существующего в базе автора повторно.
 */
public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByName(String name);
}

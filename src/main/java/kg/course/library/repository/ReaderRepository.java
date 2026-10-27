package kg.course.library.repository;

import kg.course.library.entity.Reader;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий читателей — используются стандартные методы {@link JpaRepository}.
 */
public interface ReaderRepository extends JpaRepository<Reader, String> {
}

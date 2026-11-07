package kg.course.library.repository;

import kg.course.library.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий пользователей (таблица {@code users}).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Пользователь по ИНН (уникальный столбец). */
    Optional<User> findByInn(Long inn);

    /** Есть ли пользователь с таким ИНН. */
    boolean existsByInn(Long inn);

    /** Пользователи с пагинацией, отсортированные по id. */
    Page<User> findAllByOrderByIdAsc(Pageable pageable);
}

package kg.course.library;

import kg.course.library.entity.User;
import kg.course.library.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Сервисный слой «Пользователи» — CRUD по таблице {@code users}
 * (миграция V3__add.sql). Дополнение лабораторной работы № 5.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Список пользователей с пагинацией. */
    @Transactional(readOnly = true)
    public Page<User> findAll(org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAllByOrderByIdAsc(pageable);
    }

    /** Поиск пользователя по идентификатору. */
    @Transactional(readOnly = true)
    public Optional<User> findUser(long id) {
        return userRepository.findById(id);
    }

    /** Пользователь по идентификатору; если отсутствует — {@link NotFoundException} (404). */
    @Transactional(readOnly = true)
    public User getUser(long id) {
        return findUser(id).orElseThrow(() ->
                new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    /**
     * Регистрация пользователя. ИНН уникален: повторная регистрация того же
     * ИНН — {@link IllegalStateException} (409).
     */
    @Transactional
    public User register(User user) {
        if (userRepository.existsByInn(user.getInn())) {
            throw new IllegalStateException("Пользователь с ИНН " + user.getInn() + " уже зарегистрирован");
        }
        return userRepository.save(user);
    }

    /** Изменение имени и возраста пользователя. */
    @Transactional
    public User update(long id, String name, Integer age) {
        User user = getUser(id);
        user.setName(name);
        user.setAge(age);
        return userRepository.save(user);
    }

    /** Удаление пользователя; если отсутствует — {@link NotFoundException} (404). */
    @Transactional
    public void remove(long id) {
        User user = getUser(id);
        userRepository.delete(user);
    }
}

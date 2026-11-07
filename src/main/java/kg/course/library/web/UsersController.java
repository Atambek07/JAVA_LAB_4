package kg.course.library.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.course.library.NotFoundException;
import kg.course.library.UserService;
import kg.course.library.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST-контроллер пользователей (таблица {@code users}, миграция V3__add.sql).
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Пользователи", description = "CRUD пользователей")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    /** Список пользователей с пагинацией. */
    @GetMapping
    @Operation(summary = "Список пользователей")
    public PageResponse<UserResponse> findAll(@PageableDefault(size = 10) Pageable pageable) {
        Page<User> page = userService.findAll(pageable);
        return PageResponse.from(page.map(UserResponse::from));
    }

    /** Пользователь по идентификатору. */
    @GetMapping("/{id}")
    @Operation(summary = "Пользователь по id")
    public UserResponse findById(@PathVariable long id) {
        return UserResponse.from(userService.getUser(id));
    }

    /**
     * Создание пользователя. Повторный ИНН — 409 Conflict.
     */
    @PostMapping
    @Operation(summary = "Создать пользователя")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.register(new User(request.name(), request.age(), request.inn()));
        UserResponse body = UserResponse.from(user);
        return ResponseEntity.created(URI.create("/api/users/" + user.getId())).body(body);
    }

    /** Изменение имени и возраста пользователя. */
    @PutMapping("/{id}")
    @Operation(summary = "Изменить пользователя")
    public UserResponse update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.update(id, request.name(), request.age()));
    }

    /** Удаление пользователя. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        userService.remove(id);
        return ResponseEntity.noContent().build();
    }
}

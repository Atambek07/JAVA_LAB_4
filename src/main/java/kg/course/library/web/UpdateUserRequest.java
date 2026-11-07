package kg.course.library.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Запрос на изменение пользователя (PUT /api/users/{id}).
 */
public record UpdateUserRequest(
        String name,

        @Min(value = 0, message = "Возраст не может быть отрицательным")
        @Max(value = 150, message = "Возраст не может быть больше 150")
        Integer age
) {
}

package kg.course.library.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос на создание пользователя (POST /api/users).
 */
public record CreateUserRequest(

        @NotBlank(message = "Имя пользователя обязательно")
        String name,

        @Min(value = 0, message = "Возраст не может быть отрицательным")
        Integer age,

        @NotNull(message = "ИНН обязателен")
        @Min(value = 10000000000000L, message = "ИНН должен быть 14-значным числом")
        @Max(value = 99999999999999L, message = "ИНН должен быть 14-значным числом")
        Long inn
) {
}

package kg.course.library.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос на изменение книги (PUT /api/books/{isbn}) (Задание 4).
 * <p>
 * Поле {@code version} — значение, полученное клиентом ранее (GET книги).
 * Если версия в запросе не совпадает с текущей версией в базе, возвращается
 * 409 Conflict (конкурентное изменение данных).
 * </p>
 */
public record UpdateBookRequest(

        @NotBlank(message = "Название книги обязательно")
        String title,

        @Min(value = 1450, message = "Год издания должен быть не меньше 1450")
        @Max(value = 2200, message = "Год издания слишком большой")
        int year,

        @Min(value = 1, message = "Количество экземпляров должно быть не меньше 1")
        int copies,

        @NotNull(message = "Версия книги (version) обязательна")
        Integer version
) {
}

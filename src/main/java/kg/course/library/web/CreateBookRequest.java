package kg.course.library.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Запрос на добавление книги в каталог.
 */
public record CreateBookRequest(

        @NotBlank(message = "ISBN обязателен")
        String isbn,

        @NotBlank(message = "Название книги обязательно")
        String title,

        @NotEmpty(message = "Список авторов не может быть пустым")
        List<String> authors,

        @Min(value = 1450, message = "Год издания должен быть не меньше 1450")
        int year,

        @Min(value = 1, message = "Количество экземпляров должно быть не меньше 1")
        int copies
) {
}

package kg.course.library.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Запрос на пакетную выдачу книг (POST /api/loans/batch).
 */
public record BatchIssueRequest(

        @NotBlank(message = "Номер билета обязателен")
        String cardNumber,

        @NotEmpty(message = "Список ISBN не может быть пустым")
        @Size(max = 50, message = "Список ISBN не должен превышать 50 элементов")
        List<@NotBlank(message = "ISBN обязателен") String> isbns
) {
}

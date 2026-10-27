package kg.course.library.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import kg.course.library.ReaderCategory;

/**
 * Запрос на регистрацию читателя.
 */
public record CreateReaderRequest(

        @Pattern(regexp = "R-\\d{5}", message = "Номер билета должен быть в формате R-00000")
        String cardNumber,

        @NotBlank(message = "ФИО читателя обязательно")
        String fullName,

        @NotNull(message = "Категория читателя обязательна")
        ReaderCategory category
) {
}

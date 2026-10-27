package kg.course.library.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на выдачу книги читателю.
 */
public record IssueRequest(

        @NotBlank(message = "Номер билета обязателен")
        String cardNumber,

        @NotBlank(message = "ISBN обязателен")
        String isbn
) {
}

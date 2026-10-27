package kg.course.library.web;

import kg.course.library.entity.Loan;

import java.time.LocalDate;

/**
 * Ответ API, представляющий выдачу книги.
 */
public record LoanResponse(
        long id,
        String isbn,
        String cardNumber,
        LocalDate issuedOn,
        LocalDate dueOn,
        LocalDate returnedOn,
        boolean overdue
) {

    /**
     * Преобразует JPA-сущность {@link Loan} в ответ API.
     *
     * @param loan  выдача из сервисного слоя
     * @param today текущая дата (берётся из {@code java.time.Clock})
     */
    public static LoanResponse from(Loan loan, LocalDate today) {
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getIsbn().value(),
                loan.getReader().getCardNumber(),
                loan.getIssuedOn(),
                loan.getDueOn(),
                loan.getReturnedOn(),
                loan.isOverdue(today)
        );
    }
}

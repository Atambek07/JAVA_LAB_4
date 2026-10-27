package kg.course.library;

import java.time.LocalDate;

public record Loan(long id, Isbn isbn, String cardNumber, LocalDate issuedOn, LocalDate dueOn, LocalDate returnedOn) {
    public Loan {
        if (isbn == null || cardNumber == null || issuedOn == null || dueOn == null) {
            throw new IllegalArgumentException("Обязательные поля выдачи не могут быть null");
        }
        if (dueOn.isBefore(issuedOn)) {
            throw new IllegalArgumentException("Срок возврата не может быть раньше даты выдачи");
        }
        if (returnedOn != null && returnedOn.isBefore(issuedOn)) {
            throw new IllegalArgumentException("Фактическая дата возврата не может быть раньше даты выдачи");
        }
    }

    public boolean isActive() {
        return returnedOn == null;
    }

    public boolean isOverdue(LocalDate today) {
        return isActive() && today.isAfter(dueOn);
    }

    public Loan returned(LocalDate date) {
        if (!isActive()) {
            throw new IllegalStateException("Книга по выдаче №" + id + " уже возвращена");
        }
        return new Loan(id, isbn, cardNumber, issuedOn, dueOn, date);
    }
}

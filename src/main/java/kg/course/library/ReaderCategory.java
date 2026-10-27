package kg.course.library;

import java.time.LocalDate;

public enum ReaderCategory {
    STUDENT(14, 3),
    TEACHER(30, 10),
    GUEST(7, 1);

    private final int loanDays;
    private final int maxBooks;

    ReaderCategory(int loanDays, int maxBooks) {
        this.loanDays = loanDays;
        this.maxBooks = maxBooks;
    }

    public int loanDays() { return loanDays; }
    public int maxBooks() { return maxBooks; }

    public LocalDate dueDate(LocalDate issuedOn) {
        if (issuedOn == null) {
            throw new IllegalArgumentException("Дата выдачи не может быть null");
        }
        return issuedOn.plusDays(loanDays);
    }
}

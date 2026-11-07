package kg.course.library.repository;

import jakarta.persistence.criteria.Predicate;
import kg.course.library.entity.Loan;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Спецификации фильтрации выдач (Задание 3).
 * <p>
 * Статус выдачи вычисляется на текущую дату {@code today}:
 * {@code ACTIVE} — не возвращена; {@code RETURNED} — возвращена;
 * {@code OVERDUE} — не возвращена и срок возврата истёк.
 * </p>
 */
public final class LoanSpecifications {

    private LoanSpecifications() {
    }

    /** Выдачи конкретного читателя по номеру билета. */
    public static Specification<Loan> hasCardNumber(String cardNumber) {
        return (root, query, cb) -> {
            if (cardNumber == null || cardNumber.isBlank()) {
                return null;
            }
            return cb.equal(root.get("reader").get("cardNumber"), cardNumber.trim());
        };
    }

    /** Выдачи по статусу (ACTIVE / RETURNED / OVERDUE) на дату {@code today}. */
    public static Specification<Loan> hasStatus(String status, LocalDate today) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return null;
            }
            Predicate notReturned = cb.isNull(root.get("returnedOn"));
            return switch (status.trim().toUpperCase()) {
                case "ACTIVE" -> notReturned;
                case "RETURNED" -> cb.isNotNull(root.get("returnedOn"));
                case "OVERDUE" -> cb.and(notReturned, cb.lessThan(root.get("dueOn"), today));
                default -> null;
            };
        };
    }

    /** Дата выдачи не раньше {@code from} (если задана). */
    public static Specification<Loan> issuedFrom(LocalDate from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("issuedOn"), from);
    }

    /** Дата выдачи не позже {@code to} (если задана). */
    public static Specification<Loan> issuedTo(LocalDate to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get("issuedOn"), to);
    }

    /** Комбинация всех фильтров; каждый фильтр учитывается, только если параметр задан. */
    public static Specification<Loan> search(String cardNumber, String status,
                                             LocalDate from, LocalDate to, LocalDate today) {
        return Specification.where(hasCardNumber(cardNumber))
                .and(hasStatus(status, today))
                .and(issuedFrom(from))
                .and(issuedTo(to));
    }
}

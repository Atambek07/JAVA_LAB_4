package kg.course.library.repository;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import kg.course.library.entity.Book;
import kg.course.library.entity.Loan;
import org.springframework.data.jpa.domain.Specification;

/**
 * Спецификации фильтрации каталога книг (Задание 2).
 * <p>
 * Каждая спецификация — самостоятельное правило; параметры {@code null} или
 * пустые НЕ добавляют ограничений (возвращают {@code null}, что Spring Data
 * трактует как «условие отсутствует»). Спецификации комбинируются через
 * {@code Specification.where(...).and(...)}.
 * </p>
 */
public final class BookSpecifications {

    private BookSpecifications() {
    }

    /** Название содержит фрагмент без учёта регистра (LOWER + LIKE). */
    public static Specification<Book> hasTitle(String fragment) {
        return (root, query, cb) -> {
            if (fragment == null || fragment.isBlank()) {
                return null;
            }
            Expression<String> lowerTitle = cb.lower(root.get("title"));
            return cb.like(lowerTitle, "%" + fragment.trim().toLowerCase() + "%");
        };
    }

    /** Книга написана автором с заданным именем (JOIN с таблицей authors, точное имя). */
    public static Specification<Book> hasAuthor(String authorName) {
        return (root, query, cb) -> {
            if (authorName == null || authorName.isBlank()) {
                return null;
            }
            var authors = root.join("authors");
            return cb.equal(cb.lower(authors.get("name")), authorName.trim().toLowerCase());
        };
    }

    /** Год издания в диапазоне yearFrom–yearTo; любая из границ может отсутствовать. */
    public static Specification<Book> yearBetween(Integer yearFrom, Integer yearTo) {
        return (root, query, cb) -> {
            if (yearFrom == null && yearTo == null) {
                return null;
            }
            jakarta.persistence.criteria.Path<Integer> year = root.get("year");
            if (yearFrom != null && yearTo != null) {
                return cb.between(year, yearFrom, yearTo);
            }
            if (yearFrom != null) {
                return cb.greaterThanOrEqualTo(year, yearFrom);
            }
            return cb.lessThanOrEqualTo(year, yearTo);
        };
    }

    /**
     * Есть свободные экземпляры: {@code copies > число активных выдач}.
     * Число активных выдач — подзапрос
     * {@code SELECT count(l.id) FROM Loan l WHERE l.book = book AND l.returnedOn IS NULL}.
     */
    public static Specification<Book> hasAvailableCopies() {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Loan> loan = sub.from(Loan.class);
            sub.select(cb.count(loan)).where(
                    cb.equal(loan.get("book"), root),
                    cb.isNull(loan.get("returnedOn")));
            return cb.greaterThan(root.get("copies"), sub);
        };
    }

    /** Комбинация всех фильтров поиска; каждый фильтр учитывается, только если параметр задан. */
    public static Specification<Book> search(String title, String author,
                                             Integer yearFrom, Integer yearTo, Boolean available) {
        Specification<Book> spec = Specification.where(hasTitle(title));
        spec = spec.and(hasAuthor(author));
        spec = spec.and(yearBetween(yearFrom, yearTo));
        if (available != null && available) {
            spec = spec.and(hasAvailableCopies());
        }
        return spec;
    }
}

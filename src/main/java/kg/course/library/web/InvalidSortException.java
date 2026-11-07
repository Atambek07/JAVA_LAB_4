package kg.course.library.web;

import kg.course.library.LibraryException;

import java.util.List;

/**
 * Сортировка по полю, не входящему в разрешённый список — 400 Bad Request (Задание 1).
 */
public class InvalidSortException extends LibraryException {

    private final List<String> invalidFields;

    public InvalidSortException(List<String> invalidFields, java.util.Set<String> allowedFields) {
        super("Сортировка разрешена только по полям " + allowedFields
                + ", получено недопустимое поле(я): " + invalidFields);
        this.invalidFields = List.copyOf(invalidFields);
    }

    public List<String> getInvalidFields() {
        return invalidFields;
    }
}

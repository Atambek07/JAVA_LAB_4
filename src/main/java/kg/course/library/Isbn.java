package kg.course.library;

import jakarta.persistence.Embeddable;

@Embeddable
public record Isbn(String value) {
    public Isbn {
        if (value == null) {
            throw new IllegalArgumentException("ISBN не может быть null");
        }
        value = value.replace(" ", "").replace("-", "");
        if (value.endsWith("x")) {
            value = value.substring(0, value.length() - 1) + "X";
        }

        boolean is10 = value.length() == 10 && value.matches("\\d{9}[\\dX]");
        boolean is13 = value.length() == 13 && value.matches("\\d{13}");

        if (!is10 && !is13) {
            throw new IllegalArgumentException("Некорректный ISBN: " + value);
        }
    }
}
package kg.course.library;

import java.time.LocalDate;
import java.util.List;

public record Book(Isbn isbn, String title, List<String> authors, int year) {
    public Book {
        if (isbn == null) {
            throw new IllegalArgumentException("ISBN не задан");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Название книги не может быть пустым");
        }
        if (authors == null || authors.isEmpty()) {
            throw new IllegalArgumentException("Список авторов не может быть пустым");
        }
        if (year > LocalDate.now().getYear()) {
            throw new IllegalArgumentException("Год издания не может превышать текущий год");
        }
        authors = List.copyOf(authors);
    }
}

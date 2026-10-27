package kg.course.library.web;

import kg.course.library.entity.Author;
import kg.course.library.entity.Book;

import java.util.List;

/**
 * Ответ API, представляющий книгу каталога с количеством экземпляров.
 */
public record BookResponse(
        String isbn,
        String title,
        List<String> authors,
        int year,
        int totalCopies,
        int availableCopies
) {

    /**
     * Преобразует JPA-сущность {@link Book} в безопасный ответ API.
     * Количество свободных экземпляров вычисляется отдельно ({@code libraryService.availableCopies}),
     * наружу доменные объекты не возвращаются.
     */
    public static BookResponse from(Book book, int availableCopies) {
        return new BookResponse(
                book.getIsbn().value(),
                book.getTitle(),
                book.getAuthors().stream().map(Author::getName).toList(),
                book.getYear(),
                book.getCopies(),
                availableCopies
        );
    }
}

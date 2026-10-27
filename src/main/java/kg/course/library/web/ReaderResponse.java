package kg.course.library.web;

import kg.course.library.ReaderCategory;
import kg.course.library.entity.Reader;

/**
 * Ответ API, представляющий читателя.
 */
public record ReaderResponse(
        String cardNumber,
        String fullName,
        ReaderCategory category
) {

    public static ReaderResponse from(Reader reader) {
        return new ReaderResponse(reader.getCardNumber(), reader.getFullName(), reader.getCategory());
    }
}

package kg.course.library;

public class BookUnavailableException extends LibraryException {
    public BookUnavailableException(String message) {
        super(message);
    }
}

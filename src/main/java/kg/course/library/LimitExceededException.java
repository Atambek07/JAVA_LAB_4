package kg.course.library;

public class LimitExceededException extends LibraryException {
    public LimitExceededException(String message) {
        super(message);
    }
}

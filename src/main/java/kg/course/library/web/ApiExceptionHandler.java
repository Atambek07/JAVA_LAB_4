package kg.course.library.web;

import kg.course.library.BookUnavailableException;
import kg.course.library.LibraryException;
import kg.course.library.LimitExceededException;
import kg.course.library.NotFoundException;
import kg.course.library.OverdueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;

/**
 * Централизованная обработка ошибок REST API в формате Problem Details (RFC 9457).
 * <p>
 * Ответ имеет Content-Type {@code application/problem+json} и стандартные поля:
 * {@code type}, {@code title}, {@code status}, {@code detail}, {@code instance}.
 * Для ошибок валидации дополнительно добавляется свойство {@code errors} —
 * список объектов {@code {field, message}}.
 * </p>
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Книга/читатель/выдача не найдены — 404 Not Found. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        return toResponse(ex, HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** Конфликт бизнес-правил (нет экземпляров, превышен лимит, просрочка) — 409 Conflict. */
    @ExceptionHandler({BookUnavailableException.class, LimitExceededException.class, OverdueException.class})
    public ResponseEntity<Object> handleConflict(LibraryException ex, WebRequest request) {
        return toResponse(ex, HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Общий конфликт (повторное создание, удаление с активными выдачами) — 409 Conflict. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalState(IllegalStateException ex, WebRequest request) {
        return toResponse(ex, HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Некорректные входные данные (например, неверный ISBN) — 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return toResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Нарушение целостности на уровне БД (например, гонка при создании
     * дубликата) — 409 Conflict. Появляется, когда наличие значения уже
     * проверено в сервисе, но параллельный запрос успел вставить дубликат.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        return toResponse(ex, HttpStatus.CONFLICT,
                "Такое значение уже существует в базе данных (конфликт целостности)", request);
    }

    /**
     * Ошибки валидации {@code @Valid} — 400 Bad Request. В ProblemDetail
     * добавляется свойство {@code errors} со списком полей и сообщений.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorItem(
                        fieldError.getField(),
                        fieldError.getDefaultMessage() == null ? "Недопустимое значение" : fieldError.getDefaultMessage()))
                .toList();

        ProblemDetail problemDetail = problemDetail(HttpStatus.BAD_REQUEST,
                "Переданные данные не прошли проверку", request);
        problemDetail.setProperty("errors", errors);

        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    /**
     * Любое необработанное исключение — 500 Internal Server Error.
     * Клиенту не раскрываются текст и трассировка стека, подробности пишутся в журнал.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex, WebRequest request) {
        log.error("Необработанное исключение: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = problemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера", request);
        return handleExceptionInternal(ex, problemDetail, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<Object> toResponse(Exception ex, HttpStatus status, String detail, WebRequest request) {
        return handleExceptionInternal(ex, problemDetail(status, detail, request), new HttpHeaders(), status, request);
    }

    /** Формирует ProblemDetail со стандартными полями и корректным instance. */
    private ProblemDetail problemDetail(HttpStatus status, String detail, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(instancePath(request)));
        return problemDetail;
    }

    /** Извлекает путь запроса для поля {@code instance}, например {@code /api/loans}. */
    private String instancePath(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith("uri=") ? description.substring(4) : description;
    }

    /** Элемент списка {@code errors} в ответе на ошибку валидации. */
    public record FieldErrorItem(String field, String message) {
    }
}

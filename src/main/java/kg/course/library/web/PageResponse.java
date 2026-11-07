package kg.course.library.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Универсальная обёртка постраничного ответа (Задание 1).
 * <p>
 * Объект {@code org.springframework.data.domain.Page} в REST-ответ не
 * возвращается напрямую (он связан с Jackson-сериализацией Spring Data и
 * может меняться между версиями): наружу отдаётся стабильный контракт —
 * содержимое страницы и метаданные пагинации.
 * </p>
 *
 * @param <T> тип элемента страницы (например, {@link BookResponse})
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /** Фабрика: строит ответ из {@link Page} Spring Data. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}

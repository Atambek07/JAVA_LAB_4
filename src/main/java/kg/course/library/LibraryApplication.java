package kg.course.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа приложения «Библиотека».
 * <p>
 * REST API лабораторной работы № 3 построено поверх доменной модели
 * лабораторной работы № 2 (пакет {@code kg.course.library}).
 * Веб-слой: пакет {@code kg.course.library.web}; конфигурация — пакет
 * {@code kg.course.library.config}.
 * </p>
 */
@SpringBootApplication
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}

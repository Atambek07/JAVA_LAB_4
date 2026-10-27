package kg.course.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Конфигурация приложения «Библиотека» (лабораторная работа № 4).
 * <p>
 * In-memory класс {@code Library}, импорт CSV через {@code ApplicationRunner}
 * и {@code LibraryReports} удалены: данные теперь загружаются миграциями Flyway,
 * бизнес-логика вынесена в {@code LibraryService}, отчёты — в
 * {@code ReportRepository}. Здесь остаётся только бин {@link Clock} — единый
 * источник текущей даты для контроллеров (в тестах заменяется на
 * {@code Clock.fixed}).
 * </p>
 */
@Configuration
public class LibraryConfig {

    /**
     * Системные часы. Текущая дата во всех контроллерах определяется как
     * {@code LocalDate.now(clock)}, чтобы дату можно было переопределить в тестах.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

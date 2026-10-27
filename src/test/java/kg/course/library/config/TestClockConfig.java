package kg.course.library.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Тестовая конфигурация: заменяет системные часы фиксированной датой,
 * чтобы тесты не зависели от реальной текущей даты.
 */
@TestConfiguration
public class TestClockConfig {

    /** Фиксированная «текущая» дата для всех интеграционных тестов. */
    public static final LocalDate FIXED_DATE = LocalDate.of(2026, 3, 20);

    /**
     * Бин назван {@code fixedClock} (а не {@code clock}), чтобы не конфликтовать
     * с производственным {@code Clock} из {@link LibraryConfig}. Аннотация
     * {@link Primary} указывает, что при внедрении по типу {@code Clock} должен
     * выбираться именно этот бин.
     */
    @Bean
    @Primary
    public Clock fixedClock() {
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = FIXED_DATE.atStartOfDay(zone).toInstant();
        return Clock.fixed(instant, zone);
    }
}

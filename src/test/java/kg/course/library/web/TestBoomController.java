package kg.course.library.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Тестовый контроллер (только для тестов): выбрасывает необработанное
 * исключение, чтобы проверить безопасный ответ 500 Internal Server Error,
 * который не должен раскрывать внутреннюю информацию клиенту.
 */
@RestController
@RequestMapping("/api/test-boom")
public class TestBoomController {

    @GetMapping
    public String boom() {
        throw new RuntimeException("секретный текст: внутренняя деталь реализации");
    }
}

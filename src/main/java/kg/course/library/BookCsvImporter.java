package kg.course.library;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookCsvImporter {
    public ImportResult importBooks(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<ImportedBook> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            int lineNum = i + 1;
            String[] parts = line.split(";", -1);
            if (parts.length != 5) {
                errors.add("Строка " + lineNum + ": неверное число полей (ожидалось 5, получено " + parts.length + ")");
                continue;
            }

            try {
                Isbn isbn = new Isbn(parts[0].trim());
                String title = parts[1].trim();
                List<String> authors = Arrays.stream(parts[2].split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

                if (authors.isEmpty()) {
                    throw new IllegalArgumentException("отсутствуют авторы");
                }

                int year;
                try {
                    year = Integer.parseInt(parts[3].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("нечисловой год '" + parts[3].trim() + "'");
                }

                int copies;
                try {
                    copies = Integer.parseInt(parts[4].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("нечисловое количество '" + parts[4].trim() + "'");
                }

                if (copies < 1) {
                    throw new IllegalArgumentException("количество экземпляров меньше 1");
                }

                Book book = new Book(isbn, title, authors, year);
                imported.add(new ImportedBook(book, copies));
            } catch (IllegalArgumentException e) {
                errors.add("Строка " + lineNum + ": " + e.getMessage());
            }
        }
        return new ImportResult(imported, errors);
    }
}

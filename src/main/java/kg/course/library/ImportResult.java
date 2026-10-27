package kg.course.library;

import java.util.List;

public record ImportResult(List<ImportedBook> imported, List<String> errors) {}

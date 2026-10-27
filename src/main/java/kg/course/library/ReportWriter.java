package kg.course.library;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReportWriter {
    public void writeOverdueReport(Library library, LocalDate today, Path file) throws IOException {
        LibraryReports reports = new LibraryReports(library);
        List<Loan> overdue = reports.overdueLoans(today);

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("cardNumber;fullName;title;dueOn;daysOverdue");
            writer.newLine();

            for (Loan loan : overdue) {
                Reader reader = library.getReader(loan.cardNumber());
                Book book = library.getBook(loan.isbn());
                long daysOverdue = ChronoUnit.DAYS.between(loan.dueOn(), today);

                writer.write(String.format("%s;%s;%s;%s;%d",
                        reader.cardNumber(),
                        reader.fullName(),
                        book.title(),
                        loan.dueOn(),
                        daysOverdue));
                writer.newLine();
            }
        }
    }
}

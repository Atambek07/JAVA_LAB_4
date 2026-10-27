package kg.course.library;

public class Magazine extends Item {
    private final int issue;

    public Magazine(String title, int year, int issue) {
        super(title, year);
        if (issue <= 0) {
            throw new IllegalArgumentException("Номер выпуска должен быть положительным");
        }
        this.issue = issue;
    }

    public int getIssue() { return issue; }

    @Override
    public int loanDays() {
        return 3;
    }

    @Override
    public String describe() {
        return "Выпуск №" + issue + " — " + super.describe();
    }
}

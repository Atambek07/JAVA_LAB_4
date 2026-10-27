package kg.course.library;

public abstract class Item implements Describable {
    private final String title;
    private final int year;

    public Item(String title, int year) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Название не может быть пустым");
        }
        if (year < 1450 || year > 2100) {
            throw new IllegalArgumentException("Год издания должен быть в диапазоне 1450–2100");
        }
        this.title = title;
        this.year = year;
    }

    public String getTitle() { return title; }
    public int getYear() { return year; }

    public abstract int loanDays();

    @Override
    public String describe() {
        return String.format("%s (%d), выдаётся на %d дн.", title, year, loanDays());
    }
}

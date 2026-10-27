package kg.course.library;

public record Reader(String cardNumber, String fullName, ReaderCategory category) {
    public Reader {
        if (cardNumber == null || !cardNumber.matches("R-\\d{5}")) {
            throw new IllegalArgumentException("Неверный формат номера билета (ожидается R-00000): " + cardNumber);
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("ФИО читателя не может быть пустым");
        }
        if (category == null) {
            throw new IllegalArgumentException("Категория читателя должна быть задана");
        }
    }
}

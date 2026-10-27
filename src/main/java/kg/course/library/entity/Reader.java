package kg.course.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kg.course.library.ReaderCategory;

/**
 * JPA-сущность «Читатель».
 * <p>
 * Первичный ключ — номер билета. Категория хранится как строка
 * ({@code @Enumerated(EnumType.STRING)}), чтобы значения в БД совпадали с
 * именами констант {@link ReaderCategory}.
 * </p>
 */
@Entity
@Table(name = "readers")
public class Reader {

    @Id
    @Column(name = "card_number", length = 7, nullable = false)
    private String cardNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ReaderCategory category;

    /** Конструктор без параметров для JPA (protected). */
    protected Reader() {
    }

    /** Открытый конструктор с проверкой аргументов. */
    public Reader(String cardNumber, String fullName, ReaderCategory category) {
        if (cardNumber == null || !cardNumber.matches("R-\\d{5}")) {
            throw new IllegalArgumentException("Неверный формат номера билета (ожидается R-00000): " + cardNumber);
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("ФИО читателя не может быть пустым");
        }
        if (category == null) {
            throw new IllegalArgumentException("Категория читателя должна быть задана");
        }
        this.cardNumber = cardNumber;
        this.fullName = fullName;
        this.category = category;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public ReaderCategory getCategory() {
        return category;
    }

    public void setCategory(ReaderCategory category) {
        this.category = category;
    }

    /** {@code equals}/{@code hashCode} — только по первичному ключу. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Reader reader = (Reader) o;
        return cardNumber != null && cardNumber.equals(reader.cardNumber);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Reader{cardNumber='" + cardNumber + "', fullName='" + fullName + "', category=" + category + "}";
    }
}

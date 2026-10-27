package kg.course.library;

import java.util.Objects;

public class ReaderCard {
    private final String number;
    private final String fullName;
    private final int maxItems;
    private int itemsOnHand;

    public ReaderCard(String number, String fullName, int maxItems) {
        if (!isValidCardNumber(number)) {
            throw new IllegalArgumentException("Неверный формат номера билета (ожидается R-00000): " + number);
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("ФИО читателя не может быть пустым");
        }
        if (maxItems < 1) {
            throw new IllegalArgumentException("Лимит книг должен быть не меньше 1");
        }
        this.number = number;
        this.fullName = fullName;
        this.maxItems = maxItems;
        this.itemsOnHand = 0;
    }

    private static boolean isValidCardNumber(String number) {
        if (number == null || number.length() != 7) return false;
        if (number.charAt(0) != 'R' || number.charAt(1) != '-') return false;
        for (int i = 2; i < 7; i++) {
            char c = number.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    public void take() {
        if (itemsOnHand >= maxItems) {
            throw new IllegalStateException("Превышен лимит книг на руках (" + maxItems + ")");
        }
        itemsOnHand++;
    }

    public void giveBack() {
        if (itemsOnHand <= 0) {
            throw new IllegalStateException("Нет книг на руках для возврата");
        }
        itemsOnHand--;
    }

    public String getNumber() { return number; }
    public String getFullName() { return fullName; }
    public int getMaxItems() { return maxItems; }
    public int getItemsOnHand() { return itemsOnHand; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReaderCard that = (ReaderCard) o;
        return Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    @Override
    public String toString() {
        return String.format("ReaderCard{number='%s', fullName='%s', items=%d/%d}",
                number, fullName, itemsOnHand, maxItems);
    }
}

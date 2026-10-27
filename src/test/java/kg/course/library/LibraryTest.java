package kg.course.library;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LibraryTest {

    @Test
    void testAverage() {
        assertEquals(3.0, LibraryUtils.average(new int[]{1, 3, 5}));
        assertThrows(IllegalArgumentException.class, () -> LibraryUtils.average(new int[]{}));
        assertThrows(IllegalArgumentException.class, () -> LibraryUtils.average(null));
    }

    @Test
    void testMax() {
        assertEquals(5, LibraryUtils.max(new int[]{1, 3, 5, 2}));
        assertThrows(IllegalArgumentException.class, () -> LibraryUtils.max(new int[]{}));
    }

    @Test
    void testSortedCopy() {
        int[] arr = {5, 2, 8, 1};
        int[] sorted = LibraryUtils.sortedCopy(arr);
        assertArrayEquals(new int[]{1, 2, 5, 8}, sorted);
        assertArrayEquals(new int[]{5, 2, 8, 1}, arr);
    }

    @Test
    void testNormalizePhone() {
        assertEquals("+996555123456", LibraryUtils.normalizePhone("0555 123 456"));
        assertEquals("+996555123456", LibraryUtils.normalizePhone("+996 (555) 123-456"));
        assertEquals("+996555123456", LibraryUtils.normalizePhone("996555123456"));
        assertThrows(IllegalArgumentException.class, () -> LibraryUtils.normalizePhone("123"));
        assertThrows(IllegalArgumentException.class, () -> LibraryUtils.normalizePhone("055512345"));
    }

    @Test
    void testReaderCard() {
        ReaderCard card = new ReaderCard("R-12345", "Test User", 2);
        assertEquals(0, card.getItemsOnHand());

        card.take();
        card.take();
        assertEquals(2, card.getItemsOnHand());

        assertThrows(IllegalStateException.class, card::take);

        card.giveBack();
        card.giveBack();
        assertEquals(0, card.getItemsOnHand());
        assertThrows(IllegalStateException.class, card::giveBack);
    }
}

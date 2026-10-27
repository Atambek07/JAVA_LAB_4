package kg.course.library;

public class LibraryUtils {
    public static double average(int[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Массив пуст или равен null");
        }
        double sum = 0;
        for (int v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    public static int max(int[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Массив пуст или равен null");
        }
        int max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    public static int[] sortedCopy(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("Массив равен null");
        }
        int[] copy = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            copy[i] = values[i];
        }
        for (int i = 0; i < copy.length - 1; i++) {
            for (int j = 0; j < copy.length - 1 - i; j++) {
                if (copy[j] > copy[j + 1]) {
                    int temp = copy[j];
                    copy[j] = copy[j + 1];
                    copy[j + 1] = temp;
                }
            }
        }
        return copy;
    }

    public static String normalizePhone(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Строка телефона равна null");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        if (sb.length() == 10 && sb.charAt(0) == '0') {
            sb.replace(0, 1, "996");
        }
        if (sb.length() != 12 || !sb.toString().startsWith("996")) {
            throw new IllegalArgumentException("Неверный формат номера телефона: " + raw);
        }
        return "+" + sb;
    }
}

package rbac;

import java.util.List;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }

        int[] colWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            colWidths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < row.length && i < headers.length; i++) {
                colWidths[i] = Math.max(colWidths[i], row[i].length());
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("\n+");
        for (int width : colWidths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        sb.append("\n");

        sb.append("|");
        for (int i = 0; i < headers.length; i++) {
            sb.append(" ").append(padRight(headers[i], colWidths[i])).append(" |");
        }
        sb.append("\n");

        sb.append("+");
        for (int width : colWidths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        sb.append("\n");

        for (String[] row : rows) {
            sb.append("|");
            for (int i = 0; i < row.length && i < headers.length; i++) {
                sb.append(" ").append(padRight(row[i], colWidths[i])).append(" |");
            }
            sb.append("\n");
        }

        sb.append("+");
        for (int width : colWidths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        sb.append("\n");

        return sb.toString();
    }

    public static String formatBox(String text) {
        if (text == null || text.isEmpty()) return "";

        String[] lines = text.split("\n");
        int maxLen = 0;
        for (String line : lines) {
            maxLen = Math.max(maxLen, line.length());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("┌").append("─".repeat(maxLen + 2)).append("┐\n");

        for (String line : lines) {
            sb.append("│ ").append(padRight(line, maxLen)).append(" │\n");
        }

        sb.append("└").append("─".repeat(maxLen + 2)).append("┘\n");
        return sb.toString();
    }

    public static String formatHeader(String text) {
        return "\n" + text + "\n" + "─".repeat(text.length()) + "\n";
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        return String.format("%-" + length + "s", text);
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        return String.format("%" + length + "s", text);
    }
}
package rbac;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public final class DateUtils {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;  
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {

    } 

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMAT);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMAT);
    }

    public static boolean isBefore(String date1, String date2) {
        try {
            return LocalDate.parse(date1, DATE_FORMAT).isBefore(LocalDate.parse(date2, DATE_FORMAT));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isAfter(String date1, String date2) {
        try {
            return LocalDate.parse(date1, DATE_FORMAT).isAfter(LocalDate.parse(date2, DATE_FORMAT));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static String addDays(String date, int days) {
        try {
            LocalDate ld = LocalDate.parse(date, DATE_FORMAT);
            return ld.plusDays(days).format(DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String formatRelativeTime(String date) {
        try {
            LocalDate target = LocalDate.parse(date, DATE_FORMAT);
            LocalDate now = LocalDate.now();

            long daysDiff = ChronoUnit.DAYS.between(now, target);

            if (daysDiff == 0) return "сегодня";
            if (daysDiff == 1) return "завтра";
            if (daysDiff == -1) return "вчера";
            if (daysDiff > 0) return "через " + daysDiff + " дней";
            return Math.abs(daysDiff) + " дней назад";
        } catch (DateTimeParseException e) {
            return "неизвестная дата";
        }
    }

    public static boolean isValidDate(String date) {
        try {
            LocalDate.parse(date, DATE_FORMAT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
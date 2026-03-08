package rbac;

import java.util.List;
import java.util.Scanner;

public final class ConsoleUtils {

    private ConsoleUtils() {
    }

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message + ": ");
            String input = scanner.nextLine().trim();
            if (required && input.isEmpty()) {
                System.out.println("Это поле обязательно для заполнения.");
                continue;
            }
            return input;
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message + " (" + min + " – " + max + "): ");
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Число должно быть в диапазоне " + min + " – " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Введите корректное число.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (да/нет): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("да") || input.equals("yes") || input.equals("д")) {
                return true;
            }
            if (input.equals("нет") || input.equals("no") || input.equals("н")) {
                return false;
            }
            System.out.println("Пожалуйста, ответьте 'да' или 'нет'.");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Список вариантов пуст");
        }

        while (true) {
            System.out.println(message + ":");
            for (int i = 0; i < options.size(); i++) {
                System.out.printf("  %d) %s%n", (i + 1), options.get(i));
            }
            System.out.print("Выберите номер (1-" + options.size() + "): ");
            String input = scanner.nextLine().trim();

            try {
                int num = Integer.parseInt(input);
                if (num >= 1 && num <= options.size()) {
                    return options.get(num - 1);
                }
                System.out.println("Неверный номер. Введите число от 1 до " + options.size() + ".");
            } catch (NumberFormatException e) {
                System.out.println("Введите корректный номер.");
            }
        }
    }
}
package rbac;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> commandDescriptions = new HashMap<>();

    public void registerCommand(String name, String description, Command command) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя команды не может быть пустым");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Описание команды не может быть пустым");
        }
        if (command == null) {
            throw new IllegalArgumentException("Команда не может быть null");
        }

        commands.put(name.trim().toLowerCase(), command);
        commandDescriptions.put(name.trim().toLowerCase(), description.trim());
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (commandName == null || commandName.isBlank()) {
            System.out.println("Команда не указана. Введите help для списка команд.");
            return;
        }

        String key = commandName.trim().toLowerCase();
        Command cmd = commands.get(key);

        if (cmd == null) {
            System.out.println("Неизвестная команда: " + commandName);
            System.out.println("Введите help для списка доступных команд.");
            return;
        }

        try {
            cmd.execute(scanner, system);
        } catch (Exception e) {
            System.out.println("Ошибка при выполнении команды '" + commandName + "': " + e.getMessage());
        }
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0];
        executeCommand(commandName, scanner, system);
    }
    
    public void printHelp() {
        System.out.println("\nДоступные команды:");
        System.out.println("───────────────────────────────────────────────");

        commandDescriptions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    System.out.printf("%-20s : %s%n", entry.getKey(), entry.getValue());
                });

        System.out.println("\nВведите команду или help для справки.");
        System.out.println("Для выхода введите exit");
    }
}

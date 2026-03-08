package rbac;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class AuditLog {

    private final List<AuditEntry> entries = new ArrayList<>();

    public record AuditEntry(
        String timestamp,     
        String action,        
        String performer,      
        String target,        
        String details        
    ) {}

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        entries.add(new AuditEntry(timestamp, action, performer, target, details));
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        return entries.stream()
                      .filter(e -> e.performer().equals(performer))
                      .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        return entries.stream()
                      .filter(e -> e.action().equals(action))
                      .collect(Collectors.toList());
    }

    public void printLog() {
        System.out.println("\nАудит-лог (все события):");
        if (entries.isEmpty()) {
            System.out.println("  Лог пуст.");
            return;
        }

        System.out.println("+────────────────────┬────────────────────┬────────────────────┬────────────────────┬───────────────────────────────────────+");
        System.out.println("| Время              | Действие           | Исполнитель        | Цель               | Подробности                           |");
        System.out.println("+────────────────────┬────────────────────┬────────────────────┬────────────────────┬───────────────────────────────────────+");

        for (AuditEntry e : entries) {
            System.out.printf("| %-18s | %-18s | %-18s | %-18s | %-37s |\n",
                    e.timestamp(), e.action(), e.performer(), e.target(), truncate(e.details(), 35));
        }

        System.out.println("+────────────────────┴────────────────────┴────────────────────┴────────────────────┴───────────────────────────────────────+");
    }

    public void saveToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("timestamp,action,performer,target,details");
            writer.newLine();

            for (AuditEntry e : entries) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        e.timestamp(), e.action(), e.performer(), e.target(), e.details().replace("\"", "\"\"")));
                writer.newLine();
            }
            System.out.println("Лог сохранён в файл: " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка сохранения лога: " + e.getMessage());
        }
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
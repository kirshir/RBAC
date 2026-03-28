package rbac;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class AuditLog {

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    public record AuditEntry(
        String timestamp,     
        String action,        
        String performer,      
        String target,        
        String details        
    ) {}

    public void log(String action, String performer, String target, String details) {
        String timestamp = DateUtils.getCurrentDateTime();
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
        System.out.println(FormatUtils.formatHeader("Аудит-лог (все события)"));

        if (entries.isEmpty()) {
            System.out.println("Лог пуст.");
            return;
        }

        String[] headers = {"Время", "Действие", "Исполнитель", "Цель", "Подробности"};
        List<String[]> rows = new ArrayList<>();

        for (AuditEntry e : entries) {
            rows.add(new String[]{
                    e.timestamp(),
                    e.action(),
                    e.performer(),
                    e.target(),
                    truncate(e.details(), 50) 
            });
        }

System.out.println(FormatUtils.formatTable(headers, rows));
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
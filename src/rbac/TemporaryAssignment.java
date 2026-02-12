package rbac;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TemporaryAssignment extends AbstractRoleAssignment {
    
    private String expiresAt;
    private boolean autoRenew;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TemporaryAssignment(
            User user, 
            Role role, 
            AssignmentMetadata metadata,
            String expiresAt,
            boolean autoRenew) {
        
        super(user, role, metadata);
        
        if (expiresAt == null || expiresAt.isBlank()) {
            throw new IllegalArgumentException("expiresAt не может быть пустым для временного назначения");
        }
        
        try {
            LocalDate.parse(expiresAt, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат expiresAt. Ожидается YYYY-MM-DD.");
        }
        
        this.expiresAt = expiresAt.trim();
        this.autoRenew = autoRenew;
    }

    @Override
    public boolean isActive() {
        return isActive(LocalDate.now());
    }

    // Перегруженный метод для тестов
    public boolean isActive(LocalDate currentDate) {
        try {
            LocalDate expiration = LocalDate.parse(expiresAt, DATE_FORMATTER);
            return !currentDate.isAfter(expiration);
        } catch (Exception e) {
            return false; 
        }
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public void extend(String newExpirationDate) {
        try {
            LocalDate.parse(newExpirationDate, DATE_FORMATTER);
            this.expiresAt = newExpirationDate.trim();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат новой даты: " + newExpirationDate);
        }
    }

    public boolean isExpired() {
        return !isActive();
    }

    public String getTimeRemaining() {
        LocalDate today = LocalDate.now();
        LocalDate exp = LocalDate.parse(expiresAt, DATE_FORMATTER);
        long days = java.time.temporal.ChronoUnit.DAYS.between(today, exp);
        
        if (days < 0) return "Истёк " + Math.abs(days) + " дней назад";
        if (days == 0) return "Истекает сегодня";
        return "Осталось " + days + " дней";
    }

    @Override
    public String summary() {
        String base = super.summary();
        return base + "\nExpires at: " + expiresAt +
               "\nAuto renew: " + (autoRenew ? "YES" : "NO") +
               "\n" + getTimeRemaining();
    }
}

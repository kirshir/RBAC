package rbac;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private String expiresAt;
    private boolean autoRenew;

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

        if (!DateUtils.isValidDate(expiresAt)) {
            throw new IllegalArgumentException("Неверный формат expiresAt. Ожидается YYYY-MM-DD.");
        }

        this.expiresAt = expiresAt.trim();
        this.autoRenew = autoRenew;
    }

    @Override
    public boolean isActive() {
        return isActive(DateUtils.getCurrentDate());
    }

    public boolean isActive(String currentDate) {
        if (!DateUtils.isValidDate(currentDate)) {
            return false;
        }

        return !DateUtils.isAfter(currentDate, expiresAt);
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void extend(String newExpirationDate) {
        if (!DateUtils.isValidDate(newExpirationDate)) {
            throw new IllegalArgumentException("Неверный формат новой даты: " + newExpirationDate);
        }

        if (DateUtils.isBefore(newExpirationDate, DateUtils.getCurrentDate())) {
            throw new IllegalArgumentException("Нельзя продлить до даты в прошлом");
        }

        this.expiresAt = newExpirationDate.trim();
    }

    public boolean isExpired() {
        return !isActive();
    }

    public String getTimeRemaining() {
        return DateUtils.formatRelativeTime(expiresAt);
    }

    @Override
    public String summary() {
        String base = super.summary();
        return base + "\nExpires at: " + expiresAt +
                "\nAuto renew: " + (autoRenew ? "YES" : "NO") +
                "\n" + getTimeRemaining();
    }
}
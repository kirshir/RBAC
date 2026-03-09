package rbac;

public record AssignmentMetadata(
    String assignedBy,
    String assignedAt,
    String reason
) {

    public static AssignmentMetadata now(String assignedBy, String reason) {
        if (assignedBy == null || assignedBy.isBlank()) {
            throw new IllegalArgumentException("assignedBy не может быть пустым");
        }

        String nowStr = DateUtils.getCurrentDate();
        return new AssignmentMetadata(
            assignedBy.trim(),
            nowStr, 
            (reason != null && !reason.isBlank()) ? reason.trim() : null
        ); 
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Assigned by: ").append(assignedBy).append("\n");
        sb.append("Assigned at: ").append(assignedAt).append("\n");
        if (reason != null && !reason.isBlank()) {
            sb.append("Reason: ").append(reason);
        } else {
            sb.append("Reason: не указана");
        }
        return sb.toString();
    }
}
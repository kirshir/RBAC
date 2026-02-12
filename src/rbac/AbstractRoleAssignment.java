package rbac;

import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;

    protected AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        if (user == null) {
            throw new IllegalArgumentException("user не может быть null");
        }
        if (role == null) {
            throw new IllegalArgumentException("role не может быть null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("metadata не может быть null");
        }

        this.assignmentId = UUID.randomUUID().toString();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return assignmentId.equals(that.assignmentId);
    }

    @Override
    public int hashCode() {
        return assignmentId.hashCode();
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(assignmentType()).append("] ");
        sb.append(role.getName());
        sb.append(" assigned to ");
        sb.append(user.username());
        sb.append(" by ");
        sb.append(metadata.assignedBy());
        sb.append(" at ");
        sb.append(metadata.assignedAt());

        if (metadata.reason() != null && !metadata.reason().isBlank()) {
            sb.append("\nReason: ").append(metadata.reason());
        }

        sb.append("\nStatus: ").append(isActive() ? "ACTIVE" : "INACTIVE");

        return sb.toString();
    }

    @Override
    public String toString() {
        return "AbstractRoleAssignment{" +
               "assignmentId='" + assignmentId + '\'' +
               ", user=" + user.username() +
               ", role=" + role.getName() +
               ", type=" + assignmentType() +
               ", active=" + isActive() +
               '}';
    }
}

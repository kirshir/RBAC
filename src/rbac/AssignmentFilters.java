package rbac;

public final class AssignmentFilters {
    private AssignmentFilters() {

    }

    public static AssignmentFilter byUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user не может быть null");
        }

        return assigment -> assigment.user().equals(user);
    }


    public static AssignmentFilter byUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username не может быть пустым");
        }
        String target = username.trim();
        return assignment -> target.equals(assignment.user().username());
    }

    
    public static AssignmentFilter byRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role не может быть null");
        }
        return assignment -> assignment.role().equals(role);
    }


    public static AssignmentFilter byRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("roleName не может быть пустым");
        }
        String target = roleName.trim();
        return assignment -> target.equals(assignment.role().getName());
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type не может быть пустым");
        }
        String target = type.trim().toUpperCase();
        if (!"PERMANENT".equals(target) && !"TEMPORARY".equals(target)) {
            throw new IllegalArgumentException("type должен быть PERMANENT или TEMPORARY");
        }
        return assignment -> target.equals(assignment.assignmentType());
    }

    public static AssignmentFilter assignedBy(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username не может быть пустым");
        }
        String target = username.trim();
        return assignment -> target.equals(assignment.metadata().assignedBy());
    }

    public static AssignmentFilter assignedAfter(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("date не может быть пустым");
        }
        String targetDate = date.trim();
        return assignment -> {
            String assignedAt = assignment.metadata().assignedAt();
            return assignedAt != null && assignedAt.compareTo(targetDate) >= 0;
        };
    }    

    public static AssignmentFilter expiringBefore(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("date не может быть пустым");
        }

        String targetDate = date.trim();

        return assignment -> {
            if (!"TEMPORARY".equals(assignment.assignmentType())) {
                return false;
            }

            TemporaryAssignment temp = (TemporaryAssignment) assignment;
            String expiresAt = temp.getExpiresAt();

            if (expiresAt == null || expiresAt.isBlank()) {
                return false;
            }

            return expiresAt.compareTo(targetDate) < 0;
        };
    }
}

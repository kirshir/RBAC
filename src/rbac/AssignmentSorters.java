package rbac;

import java.util.Comparator;

public final class AssignmentSorters {

    private AssignmentSorters() {
        
    }

    public static Comparator<RoleAssignment> byUsername() {
        return Comparator.comparing(assignment -> assignment.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return Comparator.comparing(assignment -> assignment.role().getName());
    }

    public static Comparator<RoleAssignment> byAssignmentDate() {
        return Comparator.comparing(
            assignment -> assignment.metadata().assignedAt(),
            Comparator.nullsLast(Comparator.naturalOrder())
        );
    }
}
package rbac;

public class PermanentAssignment extends AbstractRoleAssignment{
    
    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata);
    }

    private boolean revoked = false;

    @Override
    public boolean isActive() {
        return !revoked;
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }

    @Override
    public String summary() {
        String base = super.summary();
        if (revoked) {
            return base + "\nRevoked: YES (отозвано вручную)";
        }
        return base;
    }
}

package rbac;

import java.util.*;

public class Role {

    private final String id;
    private String name;
    private String description;
    private final Set<Permission> permissions = new HashSet<>();

    public Role(String name, String description) {
        this(UUID.randomUUID().toString(), name, description);
    }

    public Role(String id, String name, String description) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID роли не может быть пустым");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название роли не может быть пустым");
        }
        this.id = id;
        this.name = name.trim();
        this.description = (description != null) ? description.trim() : "";
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название роли не может быть пустым");
        }
        this.name = name.trim();
    }

    public void setDescription(String description) {
        this.description = (description != null) ? description.trim() : "";
    }

    public void addPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission не может быть null");
        }
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        if (permission != null) {
            permissions.remove(permission);
        }
    }

    public boolean hasPermission(Permission permission) {
        return permission != null && permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) {
            return false;
        }
        String upperName = permissionName.trim().toUpperCase();
        String lowerResource = resource.trim().toLowerCase();

        for (Permission p : permissions) {
            if (p.name().equals(upperName) && p.resource().equals(lowerResource)) {
                return true;
            }
        }
        return false;
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{id='" + id + "', name='" + name + "', permissions=" + permissions.size() + "}";
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append(name).append(" [ID: ").append(id).append("]\n");
        sb.append("Description: ").append(description.isEmpty() ? "<нет описания>" : description).append("\n");

        if (permissions.isEmpty()) {
            sb.append("Permissions: нет\n");
        } else {
            sb.append("Permissions (").append(permissions.size()).append("):\n");
            for (Permission p : permissions) {
                sb.append("  - ").append(p.format()).append("\n");
            }
        }
        return sb.toString();
    }
}
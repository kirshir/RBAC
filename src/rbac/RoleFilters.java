package rbac;

public final class RoleFilters {

    private RoleFilters() {
    }

    public static RoleFilter byName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }
        String target = name.trim();
        return role -> target.equals(role.getName());
    }


    public static RoleFilter byNameContains(String substring) {
        if (substring == null || substring.isBlank()) {
            return role -> true;
        }
        String sub = substring.trim().toLowerCase();
        return role -> role.getName().toLowerCase().contains(sub);
    }


    public static RoleFilter hasPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission не может быть null");
        }
        return role -> role.hasPermission(permission);
    }


    public static RoleFilter hasPermission(String permissionName, String resource) {
        if (permissionName == null || permissionName.isBlank() ||
            resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("permissionName и resource не могут быть пустыми");
        }
        String pName = permissionName.trim().toUpperCase();
        String res = resource.trim().toLowerCase();
        return role -> role.hasPermission(pName, res);
    }

    
    public static RoleFilter hasAtLeastNPermissions(int minCount) {
        if (minCount < 0) {
            throw new IllegalArgumentException("minCount не может быть отрицательным");
        }
        return role -> role.getPermissions().size() >= minCount;
    }
}
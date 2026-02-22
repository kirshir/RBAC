package rbac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {
    
    private final Map<String, Role> rolesById = new HashMap<>();

    private final Map<String, Role> rolesByName = new HashMap<>();

    @Override
    public void add(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role не может быть null");
        }

        String id = role.getId();
        String name = role.getName();

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID роли не может быть пустым");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя роли не может быть пустым");
        }

        if (rolesById.containsKey(id)) {
            throw new IllegalArgumentException("Роль с ID '" + id + "' уже существует");
        }

        String normalizedName = name.trim();
        if (rolesByName.containsKey(normalizedName)) {
            throw new IllegalArgumentException("Роль с именем '" + normalizedName + "' уже существует");
        }

        rolesById.put(id, role);
        rolesByName.put(normalizedName, role);
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) {
            return false;
        }

        String id = role.getId();
        String name = role.getName().trim();

        Role removedById = rolesById.remove(id);
        if (removedById != null) {
            rolesByName.remove(name);
            return true;
        }
        return false;
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesById.get(id.trim()));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    public Optional<Role> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesByName.get(name.trim()));
    }

    public boolean exists(String name) {
        return name != null && !name.isBlank() && rolesByName.containsKey(name.trim());
    }

    public List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return rolesById.values().stream()
                        .filter(filter::test)
                        .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        List<Role> result = findByFilter(filter);
        if (sorter != null) {
            result.sort(sorter);
        }
        return result;
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("roleName не может быть пустым");
        }
        if (permission == null) {
            throw new IllegalArgumentException("Permission не может быть null");
        }

        Role role = rolesByName.get(roleName.trim());
        if (role == null) {
            throw new IllegalArgumentException("Роль с именем '" + roleName + "' не найдена");
        }

        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("roleName не может быть пустым");
        }
        if (permission == null) {
            return;
        }

        Role role = rolesByName.get(roleName.trim());
        if (role == null) {
            throw new IllegalArgumentException("Роль с именем '" + roleName + "' не найдена");
        }

        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        if (permissionName == null || permissionName.isBlank() ||
            resource == null || resource.isBlank()) {
            return Collections.emptyList();
        }

        String pName = permissionName.trim().toUpperCase();
        String res = resource.trim().toLowerCase();

        return rolesById.values().stream()
                        .filter(role -> role.hasPermission(pName, res))
                        .collect(Collectors.toList());
    }
}

package rbac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    
    private final Map<String, RoleAssignment> assignments = new HashMap<>();

    private final Map<User, Set<RoleAssignment>> assignmentsByUser = new HashMap<>();

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment не может быть null");
        }

        String id = assignment.assignmentId();
        if (assignments.containsKey(id)) {
            throw new IllegalArgumentException("Назначение с ID '" + id + "' уже существует");
        }

        User user = assignment.user();
        Role role = assignment.role();

        if (userHasRole(user, role)) {
            throw new IllegalArgumentException("Роль '" + role.getName() + "' уже назначена пользователю '" +
                                               user.username() + "' и активна");
        }

        assignments.put(id, assignment);

        assignmentsByUser.computeIfAbsent(user, k -> new HashSet<>()).add(assignment);
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) {
            return false;
        }
        String id = assignment.assignmentId();
        RoleAssignment removed = assignments.remove(id);
        if (removed != null) {
            User user = removed.user();
            Set<RoleAssignment> userAssignments = assignmentsByUser.get(user);
            if (userAssignments != null) {
                userAssignments.remove(removed);
                if (userAssignments.isEmpty()) {
                    assignmentsByUser.remove(user);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(assignments.get(id.trim()));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
        assignmentsByUser.clear();
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }
        return findByUser(user).stream()
                               .anyMatch(a -> a.role().equals(role) && a.isActive());
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        Set<RoleAssignment> userAssignments = assignmentsByUser.get(user);
        return userAssignments != null ? new ArrayList<>(userAssignments) : Collections.emptyList();
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return Collections.emptyList();
        }
        return assignments.values().stream()
                          .filter(a -> a.role().equals(role))
                          .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return assignments.values().stream()
                          .filter(filter::test)
                          .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        List<RoleAssignment> result = findByFilter(filter);
        if (sorter != null) {
            result.sort(sorter);
        }
        return result;
    }

    public List<RoleAssignment> getActiveAssignments() {
        return findByFilter(AssignmentFilters.activeOnly());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return findByFilter(AssignmentFilters.inactiveOnly());
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }
        return getUserPermissions(user).stream()
                                       .anyMatch(p -> p.name().equals(permissionName.trim().toUpperCase()) &&
                                                      p.resource().equals(resource.trim().toLowerCase()));
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return Collections.emptySet();
        }

        Set<Permission> allPermissions = new HashSet<>();

        findByUser(user).stream()
                        .filter(RoleAssignment::isActive)
                        .map(RoleAssignment::role)
                        .flatMap(role -> role.getPermissions().stream())
                        .forEach(allPermissions::add);

        return Collections.unmodifiableSet(allPermissions);
    }

    public void revokeAssignment(String assignmentId) {
        findById(assignmentId).ifPresent(assignment -> {
            if (assignment instanceof PermanentAssignment perm) {
                perm.revoke();
            } else if (assignment instanceof TemporaryAssignment) {
                remove(assignment);
            }
        });
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        findById(assignmentId).ifPresent(assignment -> {
            if (!(assignment instanceof TemporaryAssignment temp)) {
                throw new IllegalArgumentException("Можно продлевать только временные назначения");
            }
            temp.extend(newExpirationDate);
        });
    }
}

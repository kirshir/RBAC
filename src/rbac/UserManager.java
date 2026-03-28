package rbac;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {

    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User не может быть null");
        }
        String username = user.username();
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("Пользователь с username '" + username + "' уже существует");
        }
        users.put(username, user);
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }
        return users.remove(user.username()) != null;
    }

    @Override
    public Optional<User> findById(String id) {
        return findByUsername(id);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(username.trim()));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String target = email.trim().toLowerCase();
        return users.values().stream()
                    .filter(u -> u.email().toLowerCase().equals(target))
                    .findFirst();
    }
    
    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return users.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        List<User> result = findByFilter(filter);
        if (sorter != null) {
            result.sort(sorter);
        }
        return result;
    }

    public boolean exists(String username) {
        return username != null && !username.isBlank() && users.containsKey(username.trim());
    }

    public void update(String username, String newFullName, String newEmail) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username не может быть пустым");
        }

        User existing = users.get(username.trim());
        if (existing == null) {
            throw new IllegalArgumentException("Пользователь с username '" + username + "' не найден");
        }

        
        User updated = User.validate(
            existing.username(),
            newFullName != null && !newFullName.isBlank() ? newFullName : existing.fullName(),
            newEmail != null && !newEmail.isBlank() ? newEmail : existing.email()
        );

        users.put(username.trim(), updated);
    }
}

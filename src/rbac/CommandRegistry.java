package rbac;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandRegistry {
    public static void registerAllCommands(CommandParser parser, RBACSystem system) {
        parser.registerCommand("user-list", "Вывести список всех пользователей (с фильтрами и сортировкой)",
            (scanner, sys) -> {
                String param = ConsoleUtils.promptString(scanner, "Введите 'f' для фильтров или Enter для всех", false).toLowerCase();

                UserFilter filter = null;
                Comparator<User> sorter = null;

                if ("f".equals(param)) {
                    System.out.println("Выберите фильтр:");
                    System.out.println("1. По username (содержит)");
                    System.out.println("2. По email (содержит)");
                    System.out.println("3. По домену email");
                    System.out.println("4. По полному имени (содержит)");
                    String choice = ConsoleUtils.promptString(scanner, "Номер фильтра", true);
                    switch (choice) {
                        case "1":
                            String sub = ConsoleUtils.promptString(scanner, "Подстрока в username", true);
                            filter = UserFilters.byUsernameContains(sub);
                            break;
                        case "2":
                            String emailSub = ConsoleUtils.promptString(scanner, "Подстрока в email", true);
                            filter = UserFilters.byEmail(emailSub);
                            break;
                        case "3":
                            String domain = ConsoleUtils.promptString(scanner, "Домен email", true);
                            filter = UserFilters.byEmailDomain(domain);
                            break;
                        case "4":
                            String nameSub = ConsoleUtils.promptString(scanner, "Подстрока в полном имени", true);
                            filter = UserFilters.byFullNameContains(nameSub);
                            break;
                        default:
                            System.out.println("Неверный выбор. Выводим всех.");
                    }
                }

                List<User> users = sys.getUserManager().findAll(filter, sorter);

                if (users.isEmpty()) {
                    System.out.println("Нет пользователей.");
                    return;
                }

                String[] headers = {"Username", "Полное имя", "Email"};
                List<String[]> rows = users.stream()
                        .map(u -> new String[]{u.username(), u.fullName(), u.email()})
                        .collect(Collectors.toList());

                System.out.println(FormatUtils.formatTable(headers, rows));
            });
        
        parser.registerCommand("user-create", "Создать нового пользователя",
            (scanner, sys)->{
                String username = ConsoleUtils.promptString(scanner, "Введите username", true);
                String fullName = ConsoleUtils.promptString(scanner, "Введите полное имя", true);
                String email = ConsoleUtils.promptString(scanner, "Введите email", true);

                try {
                    User newUser = User.validate(username, fullName, email);
                    sys.getUserManager().add(newUser);
                    System.out.println("Пользователь успешно создан: " + newUser.format());
                    sys.getAuditLog().log("USER_CREATE", sys.getCurrentUser(), username, "Создан новый пользователь");
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            });
        
        parser.registerCommand("user-view", "Просмотр информации о пользователе",
            (scanner, sys) -> {
                String username = ConsoleUtils.promptString(scanner, "Введите username", true);
                if (!ValidationUtils.isValidUsername(username)) {
                    System.out.println("Неверный формат username");
                    return;
                }

                Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                if (userOpt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }
                User user = userOpt.get();
                System.out.println("Информация о пользователе:");
                System.out.println("  Username: " + user.username());
                System.out.println("  Полное имя: " + user.fullName());
                System.out.println("  Email: " + user.email());

                List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user);
                if (assignments.isEmpty()) {
                    System.out.println("  Назначенные роли: нет");
                } else {
                    System.out.println("  Назначенные роли:");
                    assignments.forEach(a -> {
                        System.out.println("    • " + a.role().getName() + " (" + a.assignmentType() +
                                           ", " + (a.isActive() ? "активна" : "неактивна") + ")");
                    });
                }
 
                var permissions = sys.getAssignmentManager().getUserPermissions(user);
                if (!permissions.isEmpty()) {
                    System.out.println("\n  Все права:");
                    for (var perm : permissions) {
                        System.out.println("  - " + perm.format());
                    }
                }
            });
        
        parser.registerCommand("user-update", "Обновить данные пользователя",
            (scanner, sys) -> {
                String username = ConsoleUtils.promptString(scanner, "Введите username", true);
                if (!ValidationUtils.isValidUsername(username)) {
                    System.out.println("Неверный формат username");
                    return;
                }

                Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                if (userOpt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }

                String newFullName = ConsoleUtils.promptString(scanner, "Новый fullName (или Enter, чтобы оставить)", false);
                String newEmail = ConsoleUtils.promptString(scanner, "Новый email (или Enter, чтобы оставить)", false);

                if (!ValidationUtils.isValidEmail(newEmail)) {
                    System.out.println("Неверный формат email");
                    return;
                }

                try {
                    sys.getUserManager().update(username, newFullName, newEmail);
                    System.out.println("Данные пользователя обновлены.");
                    sys.getAuditLog().log("USER_UPDATE", sys.getCurrentUser(), username, "Пользователь обновлен");
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            });
        
        parser.registerCommand("user-delete", "Удалить пользователя",
            (scanner, sys) -> {
                String username = ConsoleUtils.promptString(scanner, "Username", true);
                if (!ValidationUtils.isValidUsername(username)) {
                    System.out.println("Неверный формат username");
                    return;
                }

                Optional<User> opt = sys.getUserManager().findByUsername(username);
                if (opt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }

                boolean confirmed = ConsoleUtils.promptYesNo(scanner, "Удалить пользователя и все его назначения?");
                if (!confirmed) {
                    System.out.println("Отменено.");
                    return;
                }

                sys.getAssignmentManager().findByUser(opt.get())
                        .forEach(sys.getAssignmentManager()::remove);

                sys.getUserManager().remove(opt.get());
                System.out.println("Пользователь удалён.");
                sys.getAuditLog().log("USER_DELETE", sys.getCurrentUser(), username, "Пользователь удален");
            });

        parser.registerCommand("user-search", "Поиск пользователей по фильтрам",
            (scanner, sys) -> {
                System.out.println("  1. По username (содержит)");
                System.out.println("  2. По email (содержит)");
                System.out.println("  3. По домену email");
                System.out.println("  4. По полному имени (содержит)");

                String choice = ConsoleUtils.promptString(scanner, "Выберите номер фильтра", true);

                UserFilter filter = null;
                switch (choice) {
                    case "1":
                        String sub = ConsoleUtils.promptString(scanner, "Подстрока в username", true);
                        filter = UserFilters.byUsernameContains(sub);
                        break;
                    case "2":
                        String emailSub = ConsoleUtils.promptString(scanner, "Подстрока в email", true);
                        filter = UserFilters.byEmail(emailSub);
                        break;
                    case "3":
                        String domain = ConsoleUtils.promptString(scanner, "Домен email", true);
                        filter = UserFilters.byEmailDomain(domain);
                        break;
                    case "4":
                        String nameSub = ConsoleUtils.promptString(scanner, "Подстрока в полном имени", true);
                        filter = UserFilters.byFullNameContains(nameSub);
                        break;
                    default:
                        System.out.println("Неверный выбор.");
                        return;
                }

                List<User> result = sys.getUserManager().findByFilter(filter);
                System.out.println("Результаты поиска:");
                if (result.isEmpty()) {
                    System.out.println("  Никто не найден.");
                } else {
                    result.forEach(u -> System.out.println("  " + u.format()));
                }
            });
        
        parser.registerCommand("role-list", "Вывести список всех ролей",
            (scanner, sys) -> {
                List<Role> roles = sys.getRoleManager().findAll();
                if (roles.isEmpty()) {
                    System.out.println("Нет ролей.");
                    return;
                }
                System.out.println("Список ролей:");
                roles.forEach(r -> {
                    System.out.println("  " + r.getName() + " (" + r.getPermissions().size() + " прав)");
                });
            });
        
        parser.registerCommand("role-create", "Создать новую роль",
            (scanner, sys) -> {
                String name = ConsoleUtils.promptString(scanner, "Название роли", true);
                String desc = ConsoleUtils.promptString(scanner, "Описание", false);
                
                Role role = new Role(name, desc);
                try {
                    sys.getRoleManager().add(role);
                    System.out.println("Роль создана: " + role.getName());
                    sys.getAuditLog().log("ROLE_CREATE", sys.getCurrentUser(), name, "Создана новая роль");

                    while (ConsoleUtils.promptYesNo(scanner, "Добавить право?")) {
                        String pName = ConsoleUtils.promptString(scanner, "Название права (READ, WRITE...)", true).toUpperCase();
                        String resource = ConsoleUtils.promptString(scanner, "Ресурс (users, reports...)", true).toLowerCase();
                        String pDesc = ConsoleUtils.promptString(scanner, "Описание права", false);

                        Permission p = new Permission(pName, resource, pDesc);
                        sys.getRoleManager().addPermissionToRole(name, p);
                        System.out.println("Право добавлено.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            });

        parser.registerCommand("role-view", "Просмотр роли",
            (scanner, sys) -> {
                String name = ConsoleUtils.promptString(scanner, "Введите имя роли", true);
               
                Optional<Role> opt = sys.getRoleManager().findByName(name);
                if (opt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }
                Role r = opt.get();
                System.out.println(r.format());
            });
        
        parser.registerCommand("role-update", "Обновить название или описание роли",
            (scanner, sys) -> {
                String oldName = ConsoleUtils.promptString(scanner, "Введите текущее имя роли", true);

                Optional<Role> opt = sys.getRoleManager().findByName(oldName);
                if (opt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }
                Role role = opt.get();

                String newName = ConsoleUtils.promptString(scanner, "Новое название (Enter — оставить)", false);
                String newDesc = ConsoleUtils.promptString(scanner, "Новое описание (Enter — оставить)", false);

                if (!newName.isEmpty() && !newName.equals(role.getName()) && sys.getRoleManager().exists(newName)) {
                    System.out.println("Ошибка: роль с именем '" + newName + "' уже существует");
                    return;
                }

                try {
                    Role updatedRole = new Role(newName, newDesc);
                    
                    for (Permission perm : role.getPermissions()) {
                        updatedRole.addPermission(perm);
                    }
                    
                    sys.getRoleManager().remove(role);
                    sys.getRoleManager().add(updatedRole);
                    
                    System.out.println("Роль успешно обновлена");
                    sys.getAuditLog().log("ROLE_UPDATE", sys.getCurrentUser(), newName, "Роль обновлена");
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка при обновлении: " + e.getMessage());
                }
            });
        
        parser.registerCommand("role-delete", "Удалить роль",
            (scanner, sys) -> {
                String name = ConsoleUtils.promptString(scanner, "Введите имя роли", true);

                Optional<Role> opt = sys.getRoleManager().findByName(name);
                if (opt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }

                List<RoleAssignment> assigns = sys.getAssignmentManager().findByRole(opt.get());
                if (!assigns.isEmpty()) {
                    System.out.println("Внимание! Роль назначена " + assigns.size() + " пользователям:");
                    assigns.forEach(a -> System.out.println(" - " + a.user().username()));
                }

                boolean confirmed = ConsoleUtils.promptYesNo(scanner, "Удалить роль?");
                if (!confirmed) {
                    System.out.println("Удаление отменено.");
                    return;
                }

                sys.getRoleManager().remove(opt.get());
                System.out.println("Роль удалена.");
                sys.getAuditLog().log("ROLE_DELETE", sys.getCurrentUser(), name, "Удалена роль");
            });

        parser.registerCommand("role-add-permission", "Добавить право к роли",
            (scanner, sys) -> {
                String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);

                Optional<Role> opt = sys.getRoleManager().findByName(roleName);
                if (opt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }

                String pName = ConsoleUtils.promptString(scanner, "Название права (READ, WRITE...)", true).toUpperCase();
                String resource = ConsoleUtils.promptString(scanner, "Ресурс (users, reports...)", true).toLowerCase();
                String pDesc = ConsoleUtils.promptString(scanner, "Описание права", false);

                Permission p = new Permission(pName, resource, pDesc);
                sys.getRoleManager().addPermissionToRole(roleName, p);
                System.out.println("Право добавлено к роли " + roleName);
                sys.getAuditLog().log("PERMISSION_ADD", sys.getCurrentUser(), roleName, "Добавлено право " + pName + " на " + resource);
            });
        
        parser.registerCommand("role-remove-permission", "Удалить право из роли",
            (scanner, sys) -> {
                String roleName = ConsoleUtils.promptString(scanner, "Введите имя роли", true);

                Optional<Role> opt = sys.getRoleManager().findByName(roleName);
                if (opt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }
                Role role = opt.get();

                if (role.getPermissions().isEmpty()) {
                    System.out.println("У роли нет прав.");
                    return;
                }

                System.out.println("Список прав роли " + roleName + ":");
                List<Permission> perms = new ArrayList<>(role.getPermissions());
                for (int i = 0; i < perms.size(); i++) {
                    System.out.println(" " + (i + 1) + ") " + perms.get(i).format());
                }

                int index = ConsoleUtils.promptInt(scanner, "Номер права для удаления", 1, perms.size());
                Permission toRemove = perms.get(index - 1);

                sys.getRoleManager().removePermissionFromRole(roleName, toRemove);
                System.out.println("Право удалено.");
                sys.getAuditLog().log("PERMISSION_REMOVE", sys.getCurrentUser(), roleName, "Удалено право " + toRemove.name() + " на " + toRemove.resource());
            });
            
        parser.registerCommand("role-search", "Поиск ролей по фильтрам",
            (scanner, sys) -> {
                System.out.println("Доступные фильтры:");
                System.out.println("  1. По имени (содержит)");
                System.out.println("  2. По наличию конкретного права");
                System.out.println("  3. По минимальному количеству прав");

                String choice = ConsoleUtils.promptString(scanner, "Выберите номер фильтра", true);

                RoleFilter filter = null;
                switch (choice) {
                    case "1":
                        String sub = ConsoleUtils.promptString(scanner, "Подстрока в имени роли", true);
                        filter = RoleFilters.byNameContains(sub);
                        break;
                    case "2":
                        String pName = ConsoleUtils.promptString(scanner, "Название права", true).toUpperCase();
                        String resource = ConsoleUtils.promptString(scanner, "Ресурс", true).toLowerCase();
                        filter = RoleFilters.hasPermission(pName, resource);
                        break;
                    case "3":
                        int min = ConsoleUtils.promptInt(scanner, "Минимальное количество прав", 0, 100);
                        filter = RoleFilters.hasAtLeastNPermissions(min);
                        break;
                    default:
                        System.out.println("Неверный выбор.");
                        return;
                }

                List<Role> result = sys.getRoleManager().findByFilter(filter);
                System.out.println("Результаты поиска:");
                if (result.isEmpty()) {
                    System.out.println("  Ролей не найдено.");
                } else {
                    result.forEach(r -> System.out.println("  " + r.getName() + " (" + r.getPermissions().size() + " прав)"));
                }
            });
        
        parser.registerCommand("assign-role", "Назначить роль пользователю",
            (scanner, sys) -> {
                String username = ConsoleUtils.promptString(scanner, "Username пользователя", true);
                if (!ValidationUtils.isValidUsername(username)) {
                    System.out.println("Неверный формат username");
                    return;
                }

                Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                if (userOpt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }
                User user = userOpt.get();

                List<Role> roles = sys.getRoleManager().findAll();
                if (roles.isEmpty()) {
                    System.out.println("Нет доступных ролей.");
                    return;
                }

                Role role = ConsoleUtils.promptChoice(scanner, "Выберите роль", roles);

                String type = ConsoleUtils.promptString(scanner, "Тип назначения (permanent / temporary)", true).toLowerCase();

                AssignmentMetadata meta = AssignmentMetadata.now(sys.getCurrentUser(), "Назначено через консоль");
                
                RoleAssignment assignment;
                if (type.equals("permanent")) {
                    assignment = new PermanentAssignment(user, role, meta);
                } else if (type.equals("temporary")) {
                    String expires = ConsoleUtils.promptString(scanner, "Дата истечения (YYYY-MM-DD)", true);
                    if (!DateUtils.isValidDate(expires)) {
                        System.out.println("Неверный формат даты (ожидается YYYY-MM-DD)");
                        return;
                    }
                    assignment = new TemporaryAssignment(user, role, meta, expires, false);    
                } else {
                    System.out.println("Неверный тип.");
                    return;
                }

                try {
                    sys.getAssignmentManager().add(assignment);
                    System.out.println("Роль назначена.");
                    sys.getAuditLog().log("ASSIGN_ROLE", sys.getCurrentUser(), user.username() + " -> " + role.getName(), "Назначена роль");
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            });
        
        parser.registerCommand("revoke-role", "Отозвать роль у пользователя",
            (scanner, sys) -> {
                String username = ConsoleUtils.promptString(scanner, "Username пользователя", true);
                if (!ValidationUtils.isValidUsername(username)) {
                    System.out.println("Неверный формат username");
                    return;
                }

                Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                if (userOpt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }
                User user = userOpt.get();

                List<RoleAssignment> assigns = sys.getAssignmentManager().findByUser(user);
                if (assigns.isEmpty()) {
                    System.out.println("У пользователя нет назначений.");
                    return;
                }

                System.out.println("Назначения пользователя " + username + ":");
                RoleAssignment selected = ConsoleUtils.promptChoice(scanner, "Выберите назначение для отзыва", assigns);

                sys.getAssignmentManager().revokeAssignment(selected.assignmentId());
                System.out.println("Назначение отозвано.");
                sys.getAuditLog().log("REVOKE_ROLE", sys.getCurrentUser(), username + " -> " + selected.role().getName(), "Отозвана роль");
            });

        parser.registerCommand("assignment-list", "Список всех назначений",
            (scanner, sys) -> {
                List<RoleAssignment> assigns = sys.getAssignmentManager().findAll();
                if (assigns.isEmpty()) {
                    System.out.println("Нет назначений.");
                    return;
                }
                System.out.println("Все назначения:");
                assigns.forEach(a -> {
                    System.out.println("  " + a.user().username() + " -> " + a.role().getName() +
                            " (" + a.assignmentType() + ", " + (a.isActive() ? "активно" : "неактивно") +
                            ", " + a.metadata().assignedAt() + ")");
                });
            });
        
        parser.registerCommand("assignment-list-user", "Назначения конкретного пользователя",
            (scanner, sys) -> {
                String username = ConsoleUtils.promptString(scanner, "Username пользователя", true);
                if (!ValidationUtils.isValidUsername(username)) {
                    System.out.println("Неверный формат username");
                    return;
                }

                Optional<User> opt = sys.getUserManager().findByUsername(username);
                if (opt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }

                List<RoleAssignment> assigns = sys.getAssignmentManager().findByUser(opt.get());
                if (assigns.isEmpty()) {
                    System.out.println("Нет назначений.");
                    return;
                }
                System.out.println("Назначения для " + username + ":");
                assigns.forEach(a -> System.out.println(" - " + a.role().getName() + 
                    " (" + a.assignmentType() + ", " + (a.isActive() ? "активно" : "неактивно") + ")"));
            });
        
        parser.registerCommand("assignment-list-role", "Пользователи с конкретной ролью",
            (scanner, sys) -> {
                String roleName = ConsoleUtils.promptString(scanner, "Имя роли", true);

                Optional<Role> opt = sys.getRoleManager().findByName(roleName);
                if (opt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }

                List<RoleAssignment> assigns = sys.getAssignmentManager().findByRole(opt.get());
                if (assigns.isEmpty()) {
                    System.out.println("Никто не имеет эту роль.");
                    return;
                }

                System.out.println("Пользователи с ролью " + roleName + ":");
                assigns.forEach(a -> System.out.println("  - " + a.user().username() +
                        " (" + (a.isActive() ? "активно" : "неактивно") + ")"));
            });

            
        parser.registerCommand("assignment-active", "Только активные назначения",
            (scanner, sys) -> {
                List<RoleAssignment> active = sys.getAssignmentManager().getActiveAssignments();
                if (active.isEmpty()) {
                    System.out.println("Нет активных назначений.");
                    return;
                }
                System.out.println("Активные назначения:");
                active.forEach(a -> System.out.println("  " + a.user().username() + " -> " + a.role().getName()));
            });

        parser.registerCommand("assignment-expired", "Истёкшие временные назначения",
            (scanner, sys) -> {
                List<RoleAssignment> expired = sys.getAssignmentManager().findByFilter(
                        AssignmentFilters.inactiveOnly().and(AssignmentFilters.byType("TEMPORARY")));

                if (expired.isEmpty()) {
                    System.out.println("Нет истёкших назначений.");
                    return;
                }

                System.out.println("Истёкшие назначения:");
                expired.forEach(a -> System.out.println("  " + a.user().username() + " -> " +
                        a.role().getName() + " (истёк " + ((TemporaryAssignment) a).getTimeRemaining() + ")"));
            });

        parser.registerCommand("assignment-extend", "Продлить временное назначение",
            (scanner, sys) -> {
                String id = ConsoleUtils.promptString(scanner, "Введите assignment ID", true);

                Optional<RoleAssignment> opt = sys.getAssignmentManager().findById(id);
                if (opt.isEmpty()) {
                    System.out.println("Назначение не найдено.");
                    return;
                }
                RoleAssignment a = opt.get();

                if (!"TEMPORARY".equals(a.assignmentType())) {
                    System.out.println("Продлевать можно только временные назначения.");
                    return;
                }

                String newDate = ConsoleUtils.promptString(scanner, "Новая дата истечения (YYYY-MM-DD)", true);
                if (!DateUtils.isValidDate(newDate)) {
                    System.out.println("Неверный формат даты (ожидается YYYY-MM-DD)");
                    return;
                }

                try {
                    sys.getAssignmentManager().extendTemporaryAssignment(id, newDate);
                    System.out.println("Назначение продлено до " + newDate);
                    sys.getAuditLog().log("ASSIGNMENT_EXTEND", sys.getCurrentUser(), id, "Продлено до " + newDate);
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            });

        parser.registerCommand("assignment-search", "Поиск назначений по фильтрам",
            (scanner, sys) -> {
                System.out.println("Доступные фильтры для поиска назначений:");
                System.out.println("  1. По пользователю (username)");
                System.out.println("  2. По роли (имя роли)");
                System.out.println("  3. По типу (PERMANENT / TEMPORARY)");
                System.out.println("  4. Только активные");
                System.out.println("  5. Только неактивные");
                System.out.println("  6. Назначенные после даты");
                System.out.println("  7. Истекающие до даты (только временные)");

                String choice = ConsoleUtils.promptString(scanner, "Выберите номер фильтра", true);

                AssignmentFilter filter = null;

                switch (choice) {
                    case "1":
                        String username = ConsoleUtils.promptString(scanner, "Username пользователя", true);
                        if (!ValidationUtils.isValidUsername(username)) {
                            System.out.println("Неверный формат username");
                            return;
                        }
                        Optional<User> uOpt = sys.getUserManager().findByUsername(username);
                        if (uOpt.isEmpty()) {
                            System.out.println("Пользователь не найден.");
                            return;
                        }
                        filter = AssignmentFilters.byUser(uOpt.get());
                        break;

                    case "2":
                        String roleName = ConsoleUtils.promptString(scanner, "Имя роли", true);
                        Optional<Role> rOpt = sys.getRoleManager().findByName(roleName);
                        if (rOpt.isEmpty()) {
                            System.out.println("Роль не найдена.");
                            return;
                        }
                        filter = AssignmentFilters.byRole(rOpt.get());
                        break;

                    case "3":
                        String type = ConsoleUtils.promptString(scanner, "Тип (PERMANENT / TEMPORARY)", true).toUpperCase();
                        filter = AssignmentFilters.byType(type);
                        break;

                    case "4":
                        filter = AssignmentFilters.activeOnly();
                        break;

                    case "5":
                        filter = AssignmentFilters.inactiveOnly();
                        break;

                    case "6":
                        String afterDate = ConsoleUtils.promptString(scanner, "Назначенные после даты (YYYY-MM-DD)", true);
                        if (!ValidationUtils.isValidDate(afterDate)) {
                            System.out.println("Неверный формат даты");
                            return;
                        }
                        filter = AssignmentFilters.assignedAfter(afterDate);
                        break;

                    case "7":
                        String beforeDate = ConsoleUtils.promptString(scanner, "Истекающие до даты (YYYY-MM-DD)", true);
                        if (!ValidationUtils.isValidDate(beforeDate)) {
                            System.out.println("Неверный формат даты");
                            return;
                        }
                        filter = AssignmentFilters.expiringBefore(beforeDate);
                        break;

                    default:
                        System.out.println("Неверный выбор.");
                        return;
                }

                List<RoleAssignment> result = sys.getAssignmentManager().findByFilter(filter);

                System.out.println("\nРезультаты поиска:");
                if (result.isEmpty()) {
                    System.out.println("  Назначений не найдено.");
                } else {
                    result.forEach(a -> {
                        String status = a.isActive() ? "активно" : "неактивно";
                        String type = a.assignmentType();
                        String user = a.user().username();
                        String role = a.role().getName();
                        String assignedAt = a.metadata().assignedAt();
                        System.out.println("  • " + user + " -> " + role +
                                " (" + type + ", " + status + ", " + assignedAt + ")");
                    });
                }
            });

        parser.registerCommand("permissions-user", "Все права пользователя",
            (scanner, sys) -> {
                String username = ConsoleUtils.promptString(scanner, "Username", true);
                if (!ValidationUtils.isValidUsername(username)) {
                    System.out.println("Неверный формат username");
                    return;
                }   

                Optional<User> opt = sys.getUserManager().findByUsername(username);
                if (opt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }

                Set<Permission> perms = sys.getAssignmentManager().getUserPermissions(opt.get());
                if (perms.isEmpty()) {
                    System.out.println("У пользователя нет прав.");
                    return;
                }

                System.out.println("Права пользователя " + username + ":");
                perms.forEach(p -> System.out.println("  - " + p.format()));
            });

        parser.registerCommand("permissions-check", "Проверить наличие конкретного права",
            (scanner, sys) -> {
                String username = ConsoleUtils.promptString(scanner, "Username", true);
                Optional<User> uOpt = sys.getUserManager().findByUsername(username);
                if (uOpt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }

                String pName = ConsoleUtils.promptString(scanner, "Название права (READ, WRITE...)", true).toUpperCase();
                String resource = ConsoleUtils.promptString(scanner, "Ресурс", true).toLowerCase();

                boolean has = sys.getAssignmentManager().userHasPermission(uOpt.get(), pName, resource);
                System.out.println("Право " + pName + " на " + resource + ": " + (has ? "ЕСТЬ" : "НЕТ"));
            });

        parser.registerCommand("help", "Показать справку по командам",
            (scanner, sys) -> parser.printHelp());

        parser.registerCommand("stats", "Показать статистику системы",
            (scanner, sys) -> System.out.println(sys.generateStatistics()));

        parser.registerCommand("clear", "Очистить экран", 
            (scanner, sys) -> {
                for (int i = 0; i < 50; i++) {
                    System.out.println();
                }
            });

        parser.registerCommand("exit", "Выход из программы",
            (scanner, sys) -> {
                boolean confirmed = ConsoleUtils.promptYesNo(scanner, "Выйти из программы?");
                if (confirmed) {
                    System.out.println("До свидания!");
                    sys.getAuditLog().saveToFile("audit.log"); 
                    System.exit(0);
                }
                System.out.println("Выход отменён.");
            });

        parser.registerCommand("audit-log", "Просмотр лога событий",
            (scanner, sys) -> {
                sys.getAuditLog().printLog();
            });

        parser.registerCommand("save-logs-to-file", "Сохранения логов в файл", 
            (scanner, sys) -> {
                String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
                sys.getAuditLog().saveToFile(filename);
            });

        parser.registerCommand("report-users", "Отчёт по пользователям с их ролями",
            (scanner, sys) -> {
                String report = sys.getReportGenerator().generateUserReport();
                System.out.println(report);

                boolean save = ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?");
                if (save) {
                    sys.getReportGenerator().exportToFile(report, "users_report.txt");
                }
            });

        parser.registerCommand("report-roles", "Отчёт по ролям с количеством пользователей",
            (scanner, sys) -> {
                String report = sys.getReportGenerator().generateRoleReport();
                System.out.println(report);

                boolean save = ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?");
                if (save) {
                    sys.getReportGenerator().exportToFile(report, "roles_report.txt");
                }
            });

        parser.registerCommand("report-matrix", "Матрица прав (пользователи × ресурсы)",
            (scanner, sys) -> {
                String report = sys.getReportGenerator().generatePermissionMatrix();
                System.out.println(report);

                boolean save = ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?");
                if (save) {
                    sys.getReportGenerator().exportToFile(report, "permission_matrix.txt");
                }
            });
    }
}

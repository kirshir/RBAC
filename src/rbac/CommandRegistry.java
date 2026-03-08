package rbac;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CommandRegistry {
    public static void registerAllCommands(CommandParser parser, RBACSystem system) {
        parser.registerCommand("user-list", "Вывести список всех пользователей (с фильтрами и сортировкой)",
            (scanner, sys) -> {
                System.out.println("Список пользователей (введите 'f' для фильтров или Enter для всех): ");
                String param = scanner.nextLine().trim().toLowerCase();

                UserFilter filter = null;
                Comparator<User> sorter = null;

                if ("f".equals(param)) {
                    System.out.println("Выберите фильтр:");
                    System.out.println("1. По username (содержит)");
                    System.out.println("2. По email (содержит)");
                    System.out.println("3. По домену email");
                    System.out.println("4. По полному имени (содержит)");
                    String choice = scanner.nextLine().trim();
                    switch (choice) {
                        case "1":
                            System.out.print("Подстрока в username: ");
                            filter = UserFilters.byUsernameContains(scanner.nextLine().trim());
                            break;
                        case "2":
                            System.out.print("Подстрока в email: ");
                            filter = UserFilters.byEmail(scanner.nextLine().trim());
                            break;
                        case "3":
                            System.out.print("Домен email: ");
                            filter = UserFilters.byEmailDomain(scanner.nextLine().trim());
                            break;
                        case "4":
                            System.out.print("Подстрока в полном имени: ");
                            filter = UserFilters.byFullNameContains(scanner.nextLine().trim());
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

                System.out.println("+────────────────────+──────────────────────+─────────────────────────────+");
                System.out.println("| Username           | Full Name            | Email                       |");
                System.out.println("+────────────────────+──────────────────────+─────────────────────────────+");

                for (User u : users) {
                    System.out.printf("| %-18s | %-20s | %-27s |\n", u.username(), u.fullName(), u.email());
                }

                System.out.println("+────────────────────+──────────────────────+─────────────────────────────+");
            });
        
        parser.registerCommand("user-create", "Создать нового пользователя",
            (scanner, sys)->{
                System.out.print("Введите username: ");
                String username = scanner.nextLine().trim();

                System.out.print("Введите полное имя: ");
                String fullName = scanner.nextLine().trim();

                System.out.print("Введите email: ");
                String email = scanner.nextLine().trim();

                try {
                    User newUser = User.validate(username, fullName, email);
                    sys.getUserManager().add(newUser);
                    System.out.println("Пользователь успешно создан: " + newUser.format());
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            });
        
        parser.registerCommand("user-view", "Просмотр информации о пользователе",
            (scanner, sys) -> {
                System.out.print("Введите username: ");
                String username = scanner.nextLine().trim();
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
                System.out.print("Введите username: ");
                String username = scanner.nextLine().trim();
                if (!ValidationUtils.isValidUsername(username)) {
                    System.out.println("Неверный формат username");
                    return;
                }

                Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                if (userOpt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }

                System.out.print("Новый fullName (или Enter, чтобы оставить): ");
                String newFullName = scanner.nextLine().trim();

                System.out.print("Новый email (или Enter, чтобы оставить): ");
                String newEmail = scanner.nextLine().trim();
                if (!ValidationUtils.isValidEmail(newEmail)) {
                    System.out.println("Неверный формат email");
                    return;
                }

                try {
                    sys.getUserManager().update(username, newFullName, newEmail);
                    System.out.println("Данные пользователя обновлены.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            });
        
        parser.registerCommand("user-delete", "Удалить пользователя",
            (scanner, sys) -> {
                System.out.print("Username: ");
                String username = scanner.nextLine().trim();
                if (!ValidationUtils.isValidUsername(username)) {
                    System.out.println("Неверный формат username");
                    return;
                }

                Optional<User> opt = sys.getUserManager().findByUsername(username);
                if (opt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }

                System.out.print("Удалить пользователя и все его назначения? (да/нет): ");
                if (!"да".equalsIgnoreCase(scanner.nextLine().trim())) {
                    System.out.println("Отменено.");
                    return;
                }

                sys.getAssignmentManager().findByUser(opt.get())
                        .forEach(sys.getAssignmentManager()::remove);

                sys.getUserManager().remove(opt.get());
                System.out.println("Пользователь удалён.");
            });

        parser.registerCommand("user-search", "Поиск пользователей по фильтрам",
            (scanner, sys) -> {
                System.out.println("  1. По username (содержит)");
                System.out.println("  2. По email (содержит)");
                System.out.println("  3. По домену email");
                System.out.println("  4. По полному имени (содержит)");

                System.out.print("Выберите номер фильтра: ");
                String choice = scanner.nextLine().trim();

                UserFilter filter = null;
                switch (choice) {
                    case "1":
                        System.out.print("Подстрока в username: ");
                        filter = UserFilters.byUsernameContains(scanner.nextLine().trim());
                        break;
                    case "2":
                        System.out.print("Подстрока в email: ");
                        filter = UserFilters.byEmail(scanner.nextLine().trim());
                        break;
                     case "3":
                        System.out.print("Домен (например @gmail.com): ");
                        filter = UserFilters.byEmailDomain(scanner.nextLine().trim());
                        break;
                    case "4":
                        System.out.print("Подстрока в полном имени: ");
                        filter = UserFilters.byFullNameContains(scanner.nextLine().trim());
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
                System.out.print("Название роли: ");
                String name = scanner.nextLine().trim();

                System.out.print("Описание: ");
                String desc = scanner.nextLine().trim();

                Role role = new Role(name, desc);
                try {
                    sys.getRoleManager().add(role);
                    System.out.println("Роль создана: " + role.getName());

                    while (true) {
                        System.out.print("Добавить право? (да/нет): ");
                        if (!"да".equalsIgnoreCase(scanner.nextLine().trim())) {
                            break;
                        }
                        System.out.print("  Название права (READ, WRITE и т.д.): ");
                        String pName = scanner.nextLine().trim().toUpperCase();

                        System.out.print("  Ресурс (users, reports и т.д.): ");
                        String resource = scanner.nextLine().trim().toLowerCase();

                        System.out.print("  Описание права: ");
                        String pDesc = scanner.nextLine().trim();

                        Permission p = new Permission(pName, resource, pDesc);
                        sys.getRoleManager().addPermissionToRole(name, p);
                        System.out.println("  Право добавлено.");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            });

        parser.registerCommand("role-view", "Просмотр роли",
            (scanner, sys) -> {
                System.out.print("Введите имя роли: ");
                String name = scanner.nextLine().trim();

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
                System.out.print("Введите текущее имя роли: ");
                String oldName = scanner.nextLine().trim();

                Optional<Role> opt = sys.getRoleManager().findByName(oldName);
                if (opt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }
                Role role = opt.get();

                System.out.print("Новое название (Enter — оставить): ");
                String newName = scanner.nextLine().trim();

                System.out.print("Новое описание (Enter — оставить): ");
                String newDesc = scanner.nextLine().trim();

                
                if (!newName.equals(role.getName()) && sys.getRoleManager().exists(newName)) {
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
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка при обновлении: " + e.getMessage());
                }
            });
        
        parser.registerCommand("role-delete", "Удалить роль",
            (scanner, sys) -> {
                System.out.print("Введите имя роли: ");
                String name = scanner.nextLine().trim();

                Optional<Role> opt = sys.getRoleManager().findByName(name);
                if (opt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }

                List<RoleAssignment> assigns = sys.getAssignmentManager().findByRole(opt.get());
                if (!assigns.isEmpty()) {
                    System.out.println("Внимание! Роль назначена " + assigns.size() + " пользователям:");
                    assigns.forEach(a -> System.out.println("  - " + a.user().username()));
                    System.out.print("Удалить всё равно? (да/нет): ");
                    if (!"да".equalsIgnoreCase(scanner.nextLine().trim())) {
                        System.out.println("Удаление отменено.");
                        return;
                    }
                }

                sys.getRoleManager().remove(opt.get());
                System.out.println("Роль удалена.");
            });

        parser.registerCommand("role-add-permission", "Добавить право к роли",
            (scanner, sys) -> {
                System.out.print("Введите имя роли: ");
                String roleName = scanner.nextLine().trim();

                Optional<Role> opt = sys.getRoleManager().findByName(roleName);
                if (opt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }

                System.out.print("Название права (READ, WRITE и т.д.): ");
                String pName = scanner.nextLine().trim().toUpperCase();

                System.out.print("Ресурс (users, reports и т.д.): ");
                String resource = scanner.nextLine().trim().toLowerCase();

                System.out.print("Описание права: ");
                String pDesc = scanner.nextLine().trim();

                Permission p = new Permission(pName, resource, pDesc);
                sys.getRoleManager().addPermissionToRole(roleName, p);
                System.out.println("Право добавлено к роли " + roleName);
            });
        
        parser.registerCommand("role-remove-permission", "Удалить право из роли",
            (scanner, sys) -> {
                System.out.print("Введите имя роли: ");
                String roleName = scanner.nextLine().trim();

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
                int index = 1;
                for (Permission p : role.getPermissions()) {
                    System.out.println("  " + index + ") " + p.format());
                    index++;
                }

                System.out.print("Номер права для удаления: ");
                String numStr = scanner.nextLine().trim();
                try {
                    int num = Integer.parseInt(numStr) - 1;
                    Permission toRemove = new ArrayList<>(role.getPermissions()).get(num);
                    sys.getRoleManager().removePermissionFromRole(roleName, toRemove);
                    System.out.println("Право удалено.");
                } catch (Exception e) {
                    System.out.println("Неверный номер.");
                }
            });
            
        parser.registerCommand("role-search", "Поиск ролей по фильтрам",
            (scanner, sys) -> {
                System.out.println("Доступные фильтры:");
                System.out.println("  1. По имени (содержит)");
                System.out.println("  2. По наличию конкретного права");
                System.out.println("  3. По минимальному количеству прав");

                System.out.print("Выберите номер фильтра: ");
                String choice = scanner.nextLine().trim();

                RoleFilter filter = null;
                switch (choice) {
                    case "1":
                        System.out.print("Подстрока в имени роли: ");
                        filter = RoleFilters.byNameContains(scanner.nextLine().trim());
                        break;
                    case "2":
                        System.out.print("Название права: ");
                        String pName = scanner.nextLine().trim().toUpperCase();
                        System.out.print("Ресурс: ");
                        String resource = scanner.nextLine().trim().toLowerCase();
                        filter = RoleFilters.hasPermission(pName, resource);
                        break;
                    case "3":
                        System.out.print("Минимальное количество прав: ");
                        try {
                            int min = Integer.parseInt(scanner.nextLine().trim());
                            filter = RoleFilters.hasAtLeastNPermissions(min);
                        } catch (NumberFormatException e) {
                            System.out.println("Неверное число.");
                            return;
                        }
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
                System.out.print("Username пользователя: ");
                String username = scanner.nextLine().trim();
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

                System.out.println("Доступные роли:");
                sys.getRoleManager().findAll().forEach(r -> System.out.println("  - " + r.getName()));
                System.out.print("Введите имя роли: ");
                String roleName = scanner.nextLine().trim();

                Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                if (roleOpt.isEmpty()) {
                    System.out.println("Роль не найдена.");
                    return;
                }
                Role role = roleOpt.get();

                System.out.print("Тип назначения (permanent / temporary): ");
                String type = scanner.nextLine().trim().toLowerCase();

                AssignmentMetadata meta = AssignmentMetadata.now(sys.getCurrentUser(), "Назначено через консоль");

                RoleAssignment assignment;
                if (type.equals("permanent")) {
                    assignment = new PermanentAssignment(user, role, meta);
                } else if (type.equals("temporary")) {
                    System.out.print("Дата истечения (YYYY-MM-DD): ");
                    String expires = scanner.nextLine().trim();
                    try {
                        LocalDate.parse(expires, DateTimeFormatter.ISO_LOCAL_DATE);
                    } catch (Exception e) {
                        System.out.println("Неверный формат даты.");
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
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            });
        
        parser.registerCommand("revoke-role", "Отозвать роль у пользователя",
            (scanner, sys) -> {
                System.out.print("Username пользователя: ");
                String username = scanner.nextLine().trim();
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
                for (int i = 0; i < assigns.size(); i++) {
                    RoleAssignment a = assigns.get(i);
                    System.out.println("  " + (i+1) + ") " + a.role().getName() +
                            " (" + a.assignmentType() + ", " +
                            (a.isActive() ? "активна" : "неактивна") + ")");
                }

                System.out.print("Номер назначения для отзыва: ");
                String numStr = scanner.nextLine().trim();
                try {
                    int num = Integer.parseInt(numStr) - 1;
                    RoleAssignment toRevoke = assigns.get(num);
                    sys.getAssignmentManager().revokeAssignment(toRevoke.assignmentId());
                    System.out.println("Назначение отозвано.");
                } catch (Exception e) {
                    System.out.println("Неверный номер.");
                }
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
                    System.out.println("  " + a.user().username() + " → " + a.role().getName() +
                            " (" + a.assignmentType() + ", " + (a.isActive() ? "активно" : "неактивно") +
                            ", " + a.metadata().assignedAt() + ")");
                });
            });
        
        parser.registerCommand("assignment-list-user", "Назначения конкретного пользователя",
            (scanner, sys) -> {
                System.out.print("Username: ");
                String username = scanner.nextLine().trim();
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
                assigns.forEach(a -> System.out.println("  - " + a.role().getName() + " (" + a.assignmentType() + ")"));
            });
        
        parser.registerCommand("assignment-list-role", "Пользователи с конкретной ролью",
            (scanner, sys) -> {
                System.out.print("Имя роли: ");
                String roleName = scanner.nextLine().trim();

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
                active.forEach(a -> System.out.println("  " + a.user().username() + " → " + a.role().getName()));
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
                expired.forEach(a -> System.out.println("  " + a.user().username() + " → " +
                        a.role().getName() + " (истёк " + ((TemporaryAssignment) a).getTimeRemaining() + ")"));
            });

        parser.registerCommand("assignment-extend", "Продлить временное назначение",
            (scanner, sys) -> {
                System.out.print("Введите assignment ID: ");
                String id = scanner.nextLine().trim();

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

                System.out.print("Новая дата истечения (YYYY-MM-DD): ");
                String newDate = scanner.nextLine().trim();
                if (!ValidationUtils.isValidDate(newDate)) {
                    System.out.println("Неверный формат даты");
                    return;
                }

                try {
                    sys.getAssignmentManager().extendTemporaryAssignment(id, newDate);
                    System.out.println("Назначение продлено до " + newDate);
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

                System.out.print("\nВыберите номер фильтра (или Enter для выхода): ");
                String choice = scanner.nextLine().trim();

                if (choice.isEmpty()) {
                    System.out.println("Поиск отменён.");
                    return;
                }

                AssignmentFilter filter = null;

                switch (choice) {
                    case "1":
                        System.out.print("Username пользователя: ");
                        String username = scanner.nextLine().trim();
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
                        System.out.print("Имя роли: ");
                        String roleName = scanner.nextLine().trim();
                        Optional<Role> rOpt = sys.getRoleManager().findByName(roleName);
                        if (rOpt.isEmpty()) {
                            System.out.println("Роль не найдена.");
                            return;
                        }
                        filter = AssignmentFilters.byRole(rOpt.get());
                        break;

                    case "3":
                        System.out.print("Тип (PERMANENT / TEMPORARY): ");
                        String type = scanner.nextLine().trim().toUpperCase();
                        filter = AssignmentFilters.byType(type);
                        break;

                    case "4":
                        filter = AssignmentFilters.activeOnly();
                        break;

                    case "5":
                        filter = AssignmentFilters.inactiveOnly();
                        break;

                    case "6":
                        System.out.print("Назначенные после даты (YYYY-MM-DD): ");
                        String afterDate = scanner.nextLine().trim();
                        if (!ValidationUtils.isValidDate(afterDate)) {
                            System.out.println("Неверный формат даты");
                            return;
                        }
                        filter = AssignmentFilters.assignedAfter(afterDate);
                        break;

                    case "7":
                        System.out.print("Истекающие до даты (YYYY-MM-DD): ");
                        String beforeDate = scanner.nextLine().trim();
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

                        System.out.println("  • " + user + " → " + role +
                                " (" + type + ", " + status + ", " + assignedAt + ")");
                    });
                }
            });

        parser.registerCommand("permissions-user", "Все права пользователя",
            (scanner, sys) -> {
                System.out.print("Username: ");
                String username = scanner.nextLine().trim();
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
                System.out.print("Username: ");
                String username = scanner.nextLine().trim();

                Optional<User> uOpt = sys.getUserManager().findByUsername(username);
                if (uOpt.isEmpty()) {
                    System.out.println("Пользователь не найден.");
                    return;
                }

                System.out.print("Название права (READ, WRITE и т.д.): ");
                String pName = scanner.nextLine().trim().toUpperCase();

                System.out.print("Ресурс: ");
                String resource = scanner.nextLine().trim().toLowerCase();

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
                System.out.print("Выйти? (да/нет): ");
                if ("да".equalsIgnoreCase(scanner.nextLine().trim())) {
                    System.out.println("До свидания!");
                    System.exit(0);
                }
                System.out.println("Выход отменён.");
            });
    }
}

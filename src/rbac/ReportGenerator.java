package rbac;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportGenerator {

    private final RBACSystem system;

    public ReportGenerator(RBACSystem system) {
        this.system = system;
    }

    public String generateUserReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Отчёт по пользователям\n");
        sb.append("──────────────────────\n\n");

        List<User> users = system.getUserManager().findAll();
        if (users.isEmpty()) {
            sb.append("Нет пользователей.\n");
            return sb.toString();
        }

        sb.append(String.format("%-20s %-25s %-30s %s%n", "Username", "Full Name", "Email", "Активные роли"));
        sb.append("───────────────────────────────────────────────────────────────────────────────────────────────\n");

        for (User u : users) {
            List<RoleAssignment> assigns = system.getAssignmentManager().findByUser(u);
            String rolesStr = assigns.stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.role().getName())
                    .collect(Collectors.joining(", "));
            if (rolesStr.isEmpty()) {
                rolesStr = "нет";
            }

            sb.append(String.format("%-20s %-25s %-30s %s%n", 
                    u.username(), 
                    u.fullName(), 
                    u.email(), 
                    rolesStr));
        }

        return sb.toString();
    }

    public String generateRoleReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Отчёт по ролям\n");
        sb.append("───────────────────\n\n");

        List<Role> roles = system.getRoleManager().findAll();
        if (roles.isEmpty()) {
            sb.append("Нет ролей.\n");
            return sb.toString();
        }

        sb.append(String.format("%-20s %-30s %-12s %s%n", "Роль", "Описание", "Прав", "Пользователей"));
        sb.append("───────────────────────────────────────────────────────────────────────────────\n");

        for (Role r : roles) {
            long userCount = system.getAssignmentManager().findByRole(r).stream()
                    .filter(RoleAssignment::isActive)
                    .count();

            sb.append(String.format("%-20s %-30s %-12d %d%n", 
                    r.getName(), 
                    r.getDescription(), 
                    r.getPermissions().size(), 
                    userCount));
        }

        return sb.toString();
    }

    public String generatePermissionMatrix() {
        StringBuilder sb = new StringBuilder();
        sb.append("Матрица прав (пользователи × ресурсы)\n");
        sb.append("─────────────────────────────────────\n\n");

        List<User> users = system.getUserManager().findAll();
        if (users.isEmpty()) {
            sb.append("Нет пользователей.\n");
            return sb.toString();
        }

        Set<String> resources = new HashSet<>();
        system.getAssignmentManager().getActiveAssignments().forEach(a -> {
            a.role().getPermissions().forEach(p -> resources.add(p.resource()));
        });

        if (resources.isEmpty()) {
            sb.append("Нет прав доступа.\n");
            return sb.toString();
        }

        sb.append(String.format("%-20s", "Username"));
        for (String res : resources) {
            sb.append(String.format("%-12s", res));
        }
        sb.append("\n");
        sb.append("-".repeat(20 + resources.size() * 12)).append("\n");

        for (User u : users) {
            sb.append(String.format("%-20s", u.username()));

            for (String res : resources) {
                boolean hasRead = system.getAssignmentManager().userHasPermission(u, "READ", res);
                boolean hasWrite = system.getAssignmentManager().userHasPermission(u, "WRITE", res);
                String cell = "";
                if (hasRead) cell += "R";
                if (hasWrite) cell += "W";
                sb.append(String.format("%-12s", cell.isEmpty() ? "-" : cell));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(report);
            System.out.println("Отчёт сохранён в файл: " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка сохранения отчёта: " + e.getMessage());
        }
    }
}
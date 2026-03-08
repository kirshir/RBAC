package rbac;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
        sb.append(FormatUtils.formatHeader("Отчёт по пользователям"));

        List<User> users = system.getUserManager().findAll();
        if (users.isEmpty()) {
            sb.append("Нет пользователей.\n");
            return sb.toString();
        }

        String[] headers = {"Username", "Полное имя", "Email", "Активные роли"};
        List<String[]> rows = new ArrayList<>();

        for (User u : users) {
            List<RoleAssignment> assigns = system.getAssignmentManager().findByUser(u);
            String rolesStr = assigns.stream()
                    .filter(RoleAssignment::isActive)
                    .map(a -> a.role().getName())
                    .collect(Collectors.joining(", "));
            if (rolesStr.isEmpty()) {
                rolesStr = "нет";
            }

            rows.add(new String[]{
                    u.username(),
                    u.fullName(),
                    u.email(),
                    rolesStr
            });
        }

        sb.append(FormatUtils.formatTable(headers, rows));
        return sb.toString();
    }

    public String generateRoleReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("Отчёт по ролям"));

        List<Role> roles = system.getRoleManager().findAll();
        if (roles.isEmpty()) {
            sb.append("Нет ролей.\n");
            return sb.toString();
        }

        String[] headers = {"Роль", "Описание", "Кол-во прав", "Кол-во пользователей"};
        List<String[]> rows = new ArrayList<>();

        for (Role r : roles) {
            long userCount = system.getAssignmentManager().findByRole(r).stream()
                    .filter(RoleAssignment::isActive)
                    .count();

            rows.add(new String[]{
                    r.getName(),
                    r.getDescription(),
                    String.valueOf(r.getPermissions().size()),
                    String.valueOf(userCount)
            });
        }

        sb.append(FormatUtils.formatTable(headers, rows));
        return sb.toString();
    }

    public String generatePermissionMatrix() {
        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("Матрица прав (пользователи × ресурсы)"));

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

        List<String> headerList = new ArrayList<>();
        headerList.add("Username");
        headerList.addAll(resources);

        String[] headers = headerList.toArray(new String[0]);
        List<String[]> rows = new ArrayList<>();

        for (User u : users) {
            List<String> row = new ArrayList<>();
            row.add(u.username());

            for (String res : resources) {
                boolean hasRead = system.getAssignmentManager().userHasPermission(u, "READ", res);
                boolean hasWrite = system.getAssignmentManager().userHasPermission(u, "WRITE", res);
                String cell = "";
                if (hasRead) cell += "R";
                if (hasWrite) cell += "W";
                row.add(cell.isEmpty() ? "-" : cell);
            }

            rows.add(row.toArray(new String[0]));
        }

        sb.append(FormatUtils.formatTable(headers, rows));
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
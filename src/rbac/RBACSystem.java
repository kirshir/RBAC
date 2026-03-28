package rbac;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RBACSystem {
    private final UserManager userManager = new UserManager();
    private final RoleManager roleManager = new RoleManager();
    private final AssignmentManager assignmentManager = new AssignmentManager();
    private final AuditLog auditLog = new AuditLog();
    private final ReportGenerator reportGenerator = new ReportGenerator(this);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private String currentUser = "system";
 
    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public void setCurrentUser(String username) {
        this.currentUser = username != null ? username.trim() : "system";
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public ReportGenerator getReportGenerator() {
        return reportGenerator;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void shutdown() {
        executorService.shutdown();
        scheduler.shutdown();
        auditLog.shutdown();
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public void startPeriodicTasks() {
        scheduler.scheduleAtFixedRate(this::checkExpiredAssignments, 0, 60, TimeUnit.SECONDS);
    }

    private void checkExpiredAssignments() {
        List<RoleAssignment> allAssignments = assignmentManager.findAll();
        int expiredCount = 0;
        
        for (RoleAssignment assignment : allAssignments) {
            if (assignment instanceof TemporaryAssignment tempAssignment &&
                !tempAssignment.isActive() &&
                assignmentManager.findById(assignment.assignmentId()).isPresent()) {
                assignmentManager.remove(assignment);
                expiredCount++;
                
                auditLog.log("EXPIRED_ASSIGNMENT_REMOVED", "system",
                    assignment.user().username() + " -> " + assignment.role().getName(),
                    "Временное назначение истекло и удалено");
            }
        }
        
        auditLog.log("STATISTICS_REPORT", "system", "periodic-task",
            String.format("Проверка истёкших назначений: %d истёкших назначений обработано, всего активных: %d",
                expiredCount, assignmentManager.getActiveAssignments().size()));
    }

    public void initialize() {
        Permission readUsers  = new Permission("READ",  "users",   "Просмотр списка пользователей");
        Permission writeUsers = new Permission("WRITE", "users",   "Создание и редактирование");
        Permission deleteUsers = new Permission("DELETE", "users", "Удаление пользователей");
        Permission readReports = new Permission("READ",  "reports", "Просмотр отчётов");

        Role admin = new Role("Administrator", "Полный доступ ко всей системе");
        admin.addPermission(readUsers);
        admin.addPermission(writeUsers);
        admin.addPermission(deleteUsers);
        admin.addPermission(readReports);

        Role manager = new Role("Manager", "Управление контентом");
        manager.addPermission(readUsers);
        manager.addPermission(writeUsers);
        manager.addPermission(readReports);

        Role viewer = new Role("Viewer", "Только просмотр");
        viewer.addPermission(readUsers);
        viewer.addPermission(readReports);

        roleManager.add(admin);
        roleManager.add(manager);
        roleManager.add(viewer);

        // создание администратора
        User adminUser = User.validate("admin", "System Administrator", "admin@rbac.local");
        userManager.add(adminUser);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "Initial admin setup");
        PermanentAssignment adminAssignment = new PermanentAssignment(adminUser, admin, meta);
        assignmentManager.add(adminAssignment);
    }

    public String generateStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("Статистика системы:\n");
        sb.append("────────────────────\n");
        sb.append("Пользователей: ").append(userManager.count()).append("\n");
        sb.append("Ролей: ").append(roleManager.count()).append("\n");
        sb.append("Назначений всего: ").append(assignmentManager.count()).append("\n");

        long active = assignmentManager.getActiveAssignments().size();
        long expired = assignmentManager.getExpiredAssignments().size();
        sb.append("  • активных: ").append(active).append("\n");
        sb.append("  • истёкших: ").append(expired).append("\n");

        return sb.toString();
    }
}
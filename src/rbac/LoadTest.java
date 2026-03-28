package rbac;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoadTest {
    private final RBACSystem system;
    private final Random random = new Random();
    private final String[] usernames = {"user1", "user2", "user3", "user4", "user5", "user6", "user7", "user8", "user9", "user10"};
    private final String[] fullNames = {"User One", "User Two", "User Three", "User Four", "User Five", 
                                       "User Six", "User Seven", "User Eight", "User Nine", "User Ten"};
    private final String[] roleNames = {"Admin", "Editor", "Viewer", "Moderator", "Guest"};

    public static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize();
        
        
        LoadTest loadTest = new LoadTest(system);
        loadTest.runLoadTest(10, 100);
        
        system.startPeriodicTasks();
        
        try {
            Thread.sleep(1000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Проверка журнала аудита на наличие записей периодических задач...");
        var auditEntries = system.getAuditLog().getAll();
        var periodicTaskEntries = auditEntries.stream()
            .filter(entry -> entry.target().equals("periodic-task"))
            .toList();

        if (!periodicTaskEntries.isEmpty()) {
            System.out.println("Записи периодических задач в журнале:");
            for (var entry : periodicTaskEntries) {
                System.out.println("  " + entry);
            }
        } else {
            System.out.println("Не найдено записей периодических задач в журнале аудита.");
        }
        
        System.out.println("Тест нагрузки успешно завершен!");
        
        system.shutdown();
    }
    
    public LoadTest(RBACSystem system) {
        this.system = system;
    }

    public void runLoadTest(int numThreads, int operationsPerThread) {
        System.out.println("Запуск теста нагрузки с " + numThreads + " потоками, " + operationsPerThread + " операций на поток");
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        performRandomOperation(threadId, j);
                    } catch (Exception e) {
                        System.err.println("Ошибка в потоке " + threadId + ", операция " + j + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Тест нагрузки был прерван");
        }
        
        System.out.println("Тест нагрузки завершен. Проверка согласованности данных...");
        checkConsistency();
    }

    private void performRandomOperation(int threadId, int opId) {
        int operation = random.nextInt(5); // 0-4
        
        switch (operation) {
            case 0: 
                createUser(threadId, opId);
                break;
            case 1: 
                assignRole(threadId, opId);
                break;
            case 2: 
                findUser(threadId, opId);
                break;
            case 3: 
                filterUsers(threadId, opId);
                break;
            case 4: 
                getUserPermissions(threadId, opId);
                break;
        }
    }

    private void createUser(int threadId, int opId) {
        String username = usernames[random.nextInt(usernames.length)] + "_" + threadId + "_" + opId;
        String fullName = fullNames[random.nextInt(fullNames.length)] + "_" + threadId + "_" + opId;
        String email = "user" + threadId + "_" + opId + "@test.com";

        try {
            User user = User.validate(username, fullName, email);
            system.getUserManager().add(user);
        } catch (IllegalArgumentException e) {

        }
    }

    private void assignRole(int threadId, int opId) {
        List<User> users = system.getUserManager().findAll();
        if (!users.isEmpty()) {
            User user = users.get(random.nextInt(users.size()));
            
            String roleName = roleNames[random.nextInt(roleNames.length)];
            Role role = system.getRoleManager().findByName(roleName).orElse(null);
            
            if (role != null) {
                try {
                    AssignmentMetadata metadata = AssignmentMetadata.now("load-test", "Assigned in load test");
                    RoleAssignment assignment = new PermanentAssignment(user, role, metadata);
                    system.getAssignmentManager().add(assignment);
                } catch (IllegalArgumentException e) {
                
                }
            }
        }
    }

    private void findUser(int threadId, int opId) {
        List<User> users = system.getUserManager().findAll();
        if (!users.isEmpty()) {
            User user = users.get(random.nextInt(users.size()));
            system.getUserManager().findByUsername(user.username());
        }
    }

    private void filterUsers(int threadId, int opId) {
        int filterType = random.nextInt(3);
        UserFilter filter = null;
        
        switch (filterType) {
            case 0:
                filter = UserFilters.byUsernameContains("_" + threadId);
                break;
            case 1:
                filter = UserFilters.byEmailDomain("@test.com");
                break;
            case 2:
                filter = UserFilters.byFullNameContains("User");
                break;
        }
        
        system.getUserManager().findByFilter(filter);
        system.getUserManager().findByFilterParallel(filter);
    }

    private void getUserPermissions(int threadId, int opId) {
        List<User> users = system.getUserManager().findAll();
        if (!users.isEmpty()) {
            User user = users.get(random.nextInt(users.size()));
            system.getAssignmentManager().getUserPermissions(user);
        }
    }

    private void checkConsistency() {
        System.out.println("Проверка согласованности...");
        
        List<User> allUsers = system.getUserManager().findAll();
        List<RoleAssignment> allAssignments = system.getAssignmentManager().findAll();
        
        System.out.println("Всего пользователей: " + allUsers.size());
        System.out.println("Всего назначений: " + allAssignments.size());
        
        for (RoleAssignment assignment : allAssignments) {
            User assignedUser = assignment.user();
            if (!system.getUserManager().exists(assignedUser.username())) {
                System.err.println("ОШИБКА: Назначение ссылается на несуществующего пользователя: " + assignedUser.username());
            }
        }
        
        System.out.println("Проверка согласованности завершена.");
    }

}

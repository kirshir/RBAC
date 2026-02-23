package rbac;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ManagerTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    private User kirill;
    private User anna;
    private Role adminRole;
    private Role viewerRole;
    private AssignmentMetadata meta;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager();

        kirill = User.validate("kirill_92", "Kirill Ivanov", "kirill@gmail.com");
        anna   = User.validate("anna_ivanova", "Anna Ivanova", "anna@ya.ru");

        userManager.add(kirill);
        userManager.add(anna);

        adminRole = new Role("Administrator", "Полный доступ");
        adminRole.addPermission(new Permission("READ", "users", "Просмотр"));
        adminRole.addPermission(new Permission("WRITE", "users", "Редактирование"));

        viewerRole = new Role("Viewer", "Только просмотр");
        viewerRole.addPermission(new Permission("READ", "users", "Просмотр"));

        roleManager.add(adminRole);
        roleManager.add(viewerRole);

        meta = AssignmentMetadata.now("system", "Тестовое назначение");
    }

    
    @Test
    void testUserManagerFindByUsername() {
        Optional<User> found = userManager.findByUsername("kirill_92");
        assertTrue(found.isPresent());
        assertEquals("Kirill Ivanov", found.get().fullName());
    }

    @Test
    void testUserManagerFindByEmail() {
        Optional<User> found = userManager.findByEmail("anna@ya.ru");
        assertTrue(found.isPresent());
        assertEquals("anna_ivanova", found.get().username());
    }

    @Test
    void testUserManagerFilterByEmailDomain() {
        List<User> yaUsers = userManager.findByFilter(UserFilters.byEmailDomain("ya.ru"));
        assertEquals(1, yaUsers.size());
        assertEquals("anna_ivanova", yaUsers.get(0).username());
    }

    @Test
    void testUserManagerUpdate() {
        userManager.update("kirill_92", "Kirill New", "kirill.new@gmail.com");

        User updated = userManager.findByUsername("kirill_92").orElseThrow();
        assertEquals("Kirill New", updated.fullName());
        assertEquals("kirill.new@gmail.com", updated.email());
    }

    @Test
    void testUserManagerExists() {
        assertTrue(userManager.exists("kirill_92"));
        assertFalse(userManager.exists("nonexistent"));
    }

    @Test
    void testRoleManagerFindByName() {
        Optional<Role> found = roleManager.findByName("Administrator");
        assertTrue(found.isPresent());
        assertEquals("Полный доступ", found.get().getDescription());
    }

    @Test
    void testRoleManagerAddAndRemovePermission() {
        Permission manage = new Permission("MANAGE", "settings", "Управление");
        roleManager.addPermissionToRole("Administrator", manage);

        Role updated = roleManager.findByName("Administrator").orElseThrow();
        assertTrue(updated.hasPermission(manage));

        roleManager.removePermissionFromRole("Administrator", manage);
        assertFalse(updated.hasPermission(manage));
    }

    @Test
    void testRoleManagerFindRolesWithPermission() {
        List<Role> roles = roleManager.findRolesWithPermission("READ", "users");
        assertEquals(2, roles.size());
    }

    @Test
    void testAssignmentManagerAddAndFindByUser() {
        PermanentAssignment pa = new PermanentAssignment(kirill, adminRole, meta);
        assignmentManager.add(pa);

        List<RoleAssignment> kirillAssignments = assignmentManager.findByUser(kirill);
        assertEquals(1, kirillAssignments.size());
        assertEquals("Administrator", kirillAssignments.get(0).role().getName());
    }

    @Test
    void testAssignmentManagerUserHasRoleAndPermissions() {
        PermanentAssignment pa = new PermanentAssignment(kirill, adminRole, meta);
        assignmentManager.add(pa);

        assertTrue(assignmentManager.userHasRole(kirill, adminRole));
        assertTrue(assignmentManager.userHasPermission(kirill, "READ", "users"));

        Set<Permission> perms = assignmentManager.getUserPermissions(kirill);
        assertEquals(2, perms.size()); 
    }

    @Test
    void testAssignmentManagerRevokePermanent() {
        PermanentAssignment pa = new PermanentAssignment(kirill, adminRole, meta);
        assignmentManager.add(pa);

        assignmentManager.revokeAssignment(pa.assignmentId());
        assertFalse(assignmentManager.userHasRole(kirill, adminRole));
    }

    @Test
    void testAssignmentManagerExtendTemporary() {
        String futureDate = LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE);
        TemporaryAssignment ta = new TemporaryAssignment(anna, viewerRole, meta, futureDate, false);
        assignmentManager.add(ta);

        String newDate = LocalDate.now().plusDays(60).format(DateTimeFormatter.ISO_LOCAL_DATE);
        assignmentManager.extendTemporaryAssignment(ta.assignmentId(), newDate);

        TemporaryAssignment updated = (TemporaryAssignment) assignmentManager.findById(ta.assignmentId()).orElseThrow();
        assertEquals(newDate, updated.getExpiresAt());
    }

    @Test
    void testAssignmentManagerDuplicateRolePrevention() {
        PermanentAssignment pa1 = new PermanentAssignment(kirill, adminRole, meta);
        assignmentManager.add(pa1);

        PermanentAssignment pa2 = new PermanentAssignment(kirill, adminRole, meta);

        assertThrows(IllegalArgumentException.class, () -> {
            assignmentManager.add(pa2);
        });
    }
}
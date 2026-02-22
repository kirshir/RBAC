package rbac;

import java.util.Comparator;

public final class UserSorters {

    private UserSorters() {
       
    }

    public static Comparator<User> byUsername() {
        return Comparator.comparing(User::username);
    }

    public static Comparator<User> byFullName() {
        return Comparator.comparing(User::fullName);
    }

    public static Comparator<User> byEmail() {
        return Comparator.comparing(User::email);
    }
}
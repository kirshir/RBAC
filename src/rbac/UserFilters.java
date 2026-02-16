package rbac;

public final class UserFilters {
    private UserFilters() {}

    public static UserFilter byUserName(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username не может быть пустым");
        }

        String target = username.trim();
        return user -> target.equals(user.username());
    }

    
    public static UserFilter byUsernameContains(String substring) {
        if (substring == null || substring.isBlank()) {
            return user -> true;
        }

        String sub = substring.trim().toLowerCase();
        return user -> user.username().toLowerCase().contains(sub);
    }


    public static UserFilter byEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email не может быть пустым");
        }

        String target = email.trim().toLowerCase();
        return user -> target.equals(user.email().toLowerCase());
    }


    public static UserFilter byEmailDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            throw new IllegalArgumentException("domain не может быть пустым");
        }

        String normalized = domain.trim().toLowerCase();
        String finalDomain = normalized.startsWith("@") ? normalized : "@" + normalized;
    
        return user -> user.email().toLowerCase().endsWith(finalDomain);
    }


    public static UserFilter byFullNameContains(String substring) {
        if (substring == null || substring.isBlank()) {
            return user -> true;
        }
        String sub = substring.trim().toLowerCase();
        
        return user -> user.fullName().toLowerCase().contains(sub);
    }

}
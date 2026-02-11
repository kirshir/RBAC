package rbac;
import java.util.regex.Pattern;

public record User(
    String username, 
    String fullName, 
    String email
) {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static User validate(String username, String fullName, String email) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username не может быть null или пустым");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName не может быть null или пустым");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email не может быть null или пустым");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                "username должен содержать только латинские буквы, цифры и _, длина 3–20 символов. "
            );
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException(
                "email должен быть в формате xxx@yyy.zzz. "
            );
        }

        return new User(username, fullName, email);
    }

    public String format() {
        return username + " (" + fullName + ") <" + email + ">";
    }
}

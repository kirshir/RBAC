package rbac;

public class UserTest {

    public static void main(String[] args) {
        testGood("alex_007", "Alexandr Petrov", "alex.petrov@gmail.com");
        testGood("user123",  "Anna Ivanova",    "anna.ivanova@ya.ru");

        System.out.println("\n--- Ошибочные случаи ---");
        testBad("ab", "Short name", "ab@ya.ru");
        testBad("user name", "Space", "test@ya.ru");
        testBad("alex_007", "Alexandr Petrov", " ");
        testBad("alex_007", "Alexandr Petrov", "alex_007.com");
    }

    private static void testGood(String u, String f, String e) {
        try {
            User user = User.validate(u, f, e);
            System.out.println("OK " + user.format());
        } catch (Exception ex) {
            System.out.println("ОШИБКА (ожидалось успеха): " + ex.getMessage());
        }
    }

    private static void testBad(String u, String f, String e) {
        try {
            User.validate(u, f, e);
            System.out.println("ОШИБКА: должно было выбросить исключение");
        } catch (IllegalArgumentException ex) {
            System.out.println("ОК " + ex.getMessage());
        }
    }
}
package app.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordService {
    public static final String DEFAULT_PASSWORD = "1234";

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean verify(String password, String hash) {
        return password != null && hash != null && BCrypt.checkpw(password, hash);
    }
}

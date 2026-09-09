/**
 * UserAuthenticator is responsible for the deliberately simple authentication
 * model used by the academic mail-server project.
 *
 * Team project: Dave Ronic DONKENG and one teammate.
 */
import java.util.HashSet;
import java.util.Set;

public class UserAuthenticator {

    private final String localDomain;
    private final Set<String> validUsers = new HashSet<>();

    public UserAuthenticator(String localDomain) {
        this.localDomain = localDomain;

        // Synthetic portfolio accounts. The original lab-specific addresses
        // were removed from the public-facing copy.
        validUsers.add("alice@" + localDomain);
        validUsers.add("bob@" + localDomain);
    }

    public boolean authenticate(String username, String password) {
        // Intentionally simple password for the educational protocol exercise.
        if (!"password".equals(password))
            return false;

        if (!username.endsWith("@" + localDomain))
            return false;

        return validUsers.contains(username);
    }

    public String getLocalDomain() {
        return localDomain;
    }
}

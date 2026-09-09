/**
 * UserAuthenticator is responsible for authenticating users based on their
 * username and password. It maintains a set of valid users for the local domain.
 * Author
 * - dave ronic donkeng
 * - leslie lucynda tingue
 * Version: 1.0
 */
import java.util.HashSet;
import java.util.Set;

public class UserAuthenticator {

    private final String localDomain;
    private final Set<String> validUsers = new HashSet<>();

    public UserAuthenticator(String localDomain) {
        this.localDomain = localDomain;

        // Register only users belonging to this domain
        if (localDomain.equals("uliege.be")) {
            validUsers.add("alice@uliege.be");
            validUsers.add("bob@uliege.be");
        }
        else if (localDomain.equals("gembloux.uliege.be")) {
            validUsers.add("alice@gembloux.uliege.be");
            validUsers.add("bob@gembloux.uliege.be");
        }
        else if (localDomain.equals("info.uliege.be")) {
            validUsers.add("alice@info.uliege.be");
            validUsers.add("bob@info.uliege.be");
        }
    }

    public boolean authenticate(String username, String password) {

        // Password from assignment
        if (!"password".equals(password))
            return false;

        // Must be a local user from this domain
        if (!username.endsWith("@" + localDomain))
            return false;

        // Must exist in this server's local user table
        return validUsers.contains(username);
    }

    public String getLocalDomain() {
        return localDomain;
    }
}

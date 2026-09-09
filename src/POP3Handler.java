
/**
 * POP3Handler processes POP3 commands from clients,
 * managing authentication, transaction, and update phases.
 * 
 * Authors:
 * - dave donkeng ndia
 * - leslie lucynda tingue
 * Version: 1.0
 */
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class POP3Handler {

    public enum Phase {
        AUTH, TRANSACTION, UPDATE
    }

    private Phase phase = Phase.AUTH;
    private String username;
    private boolean quitRequested = false;

    // 1-based message indices marked for deletion
    private final List<Integer> toDelete = new ArrayList<>();

    /**
     * Handles a POP3 command based on the current phase.
     * 
     * @param line
     * @param out
     * @param storage
     * @param authenticator
     * @throws POP3Exception
     */
    public void handleCommand(String line,
            PrintWriter out,
            MailStorage storage,
            UserAuthenticator authenticator) throws POP3Exception {

        String[] parts = line.split(" ", 2);
        String cmd = parts[0].toUpperCase();
        String args = (parts.length > 1) ? parts[1].trim() : "";

        switch (phase) {
            case AUTH:
                handleAuth(cmd, args, out, authenticator);
                break;

            case TRANSACTION:
                handleTransaction(cmd, args, out, storage);
                break;

            case UPDATE:

                sendPOP3message(out, "+OK Goodbye");
                quitRequested = true;
                break;
        }
    }

    /**
     * handle AUTH phase commands
     * 
     * @param cmd
     * @param args
     * @param out
     * @param authenticator
     * @throws POP3Exception
     */
    private void handleAuth(String cmd,
            String args,
            PrintWriter out,
            UserAuthenticator authenticator) throws POP3Exception {

        switch (cmd) {

            case "USER":
                if (args.isEmpty())
                    throw new POP3Exception("Missing username");

                String u = args.trim().toLowerCase();
                String candidate;

                // Cas 1 : provides full email
                if (u.contains("@")) {
                    candidate = u;
                }
                // Cas 2 : add the local domain
                else {
                    candidate = u + "@" + authenticator.getLocalDomain();
                }

                /// verify domain
                if (!candidate.endsWith("@" + authenticator.getLocalDomain())) {
                    throw new POP3Exception("Invalid domain");
                }

                // store the username for the PASS command
                username = candidate;

                sendPOP3message(out, "+OK");
                break;

            case "PASS":
                if (username == null)
                    throw new POP3Exception("USER required first");

                if (!authenticator.authenticate(username, args))
                    throw new POP3Exception("Authentication failed");
                phase = Phase.TRANSACTION;

                sendPOP3message(out, "+OK Mailbox locked and ready");
                break;

            case "QUIT":

                sendPOP3message(out, "+OK Goodbye");
                quitRequested = true;
                break;
            default:
                throw new POP3Exception("");
        }
    }

    /**
     * handle TRANSACTION phase commands
     * 
     * @param cmd
     * @param args
     * @param out
     * @param storage
     * @throws POP3Exception
     */
    private void handleTransaction(String cmd,
            String args,
            PrintWriter out,
            MailStorage storage) throws POP3Exception {

        List<Email> messages = storage.getEmailsForUser(username);

        switch (cmd) {

            case "STAT": {
                int count = 0;
                int totalSize = 0;

                for (int i = 0; i < messages.size(); i++) {
                    int idx = i + 1;
                    if (!toDelete.contains(idx)) {
                        count++;
                        totalSize += messages.get(i).toRawMessage().length();
                    }
                }

                sendPOP3message(out, "+OK " + count + " " + totalSize);
                break;
            }

            case "LIST": {
                // LIST <id>
                if (!args.isEmpty()) {
                    int id = parseId(args);

                    if (id < 1 || id > messages.size() || toDelete.contains(id))
                        throw new POP3Exception("No such message");

                    int size = messages.get(id - 1).toRawMessage().length();

                    sendPOP3message(out, "+OK " + id + " " + size);
                    break;
                }

                // LIST
                int count = 0;
                int totalSize = 0;

                for (int i = 0; i < messages.size(); i++) {
                    int idx = i + 1;
                    if (!toDelete.contains(idx)) {
                        count++;
                        totalSize += messages.get(i).toRawMessage().length();
                    }
                }

                sendPOP3message(out, "+OK " + count + " messages (" + totalSize + " octets)");
                for (int i = 0; i < messages.size(); i++) {
                    int idx = i + 1;
                    if (!toDelete.contains(idx)) {

                        sendPOP3message(out, idx + " " + messages.get(i).toRawMessage().length());
                    }
                }

                sendPOP3message(out, ".");
                break;
            }

            case "RETR": {
                int id = parseId(args);

                if (id < 1 || id > messages.size() || toDelete.contains(id))
                    throw new POP3Exception("No such message");

                Email email = messages.get(id - 1);
                String raw = email.toRawMessage();

                sendPOP3message(out, "+OK " + raw.length() + " octets");

                // dot-stuffing
                for (String line : raw.split("\r?\n")) {
                    if (line.startsWith(".")) {

                        sendPOP3message(out, "." + line);
                    } else {

                        sendPOP3message(out, line);
                    }
                }

                sendPOP3message(out, ".");
                break;
            }

            case "DELE": {
                int id = parseId(args);

                if (id < 1 || id > messages.size() || toDelete.contains(id))
                    throw new POP3Exception("No such message");

                toDelete.add(id);

                sendPOP3message(out, "+OK Message marked for deletion");
                break;
            }

            case "UIDL": {
                // UIDL <id>
                if (!args.isEmpty()) {
                    int id = parseId(args);

                    if (id < 1 || id > messages.size() || toDelete.contains(id))
                        throw new POP3Exception("No such message");

                    Email email = messages.get(id - 1);

                    sendPOP3message(out, "+OK " + id + " " + email.getUid());
                    break;
                }
                // UIDL
                sendPOP3message(out, "+OK");
                for (int i = 0; i < messages.size(); i++) {
                    int idx = i + 1;
                    if (!toDelete.contains(idx)) {
                        Email email = messages.get(i);

                        sendPOP3message(out, idx + " " + email.getUid());
                    }
                }

                sendPOP3message(out, ".");
                break;
            }

            case "QUIT":
                for (int id : toDelete) {
                    storage.deleteEmailForUser(username, id);
                }

                sendPOP3message(out, "+OK Goodbye");
                quitRequested = true;
                phase = Phase.UPDATE;
                break;
            default:
                throw new POP3Exception("Unknown command");
        }
    }

    /**
     * Parse message ID from arguments.
     * 
     * @param args
     * @return
     * @throws POP3Exception
     */
    private int parseId(String args) throws POP3Exception {
        if (args.isEmpty())
            throw new POP3Exception("Missing message ID");

        try {
            return Integer.parseInt(args);
        } catch (NumberFormatException e) {
            throw new POP3Exception("Invalid message ID");
        }
    }

    /**
     * Check if QUIT command was requested.
     * 
     * @return
     */
    public boolean isQuitRequested() {
        return quitRequested;
    }

    /**
     * Send a POP3 message to the client.
     * 
     * @param out
     * @param message
     */
    public void sendPOP3message(PrintWriter out, String message) {
        out.print(message + "\r\n");
        out.flush();
    }
}

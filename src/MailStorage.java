
/**
 * MailStorage handles storage and retrieval of emails for users.
 * It supports operations for both POP3 and IMAP protocols.
 * It maintains mailboxes for users and provides methods to
 * store, list, delete emails..
 * Author
 * - dave ronic donkeng
 * - leslie lucynda tingue
 * Version: 1.0
 */
import java.util.*;

public class MailStorage {

    private final String localDomain;

    // Map: "user@domain" -> (mailboxName -> Mailbox)
    private final Map<String, Map<String, Mailbox>> userMailboxes = new HashMap<>();

    // user@domain -> subscribed mailboxes
    private final Map<String, Set<String>> subscriptions = new HashMap<>();

    public MailStorage(String localDomain) {
        this.localDomain = localDomain;
    }

    // Get or create mailbox for user
    public synchronized Mailbox getOrCreateMailbox(String fullUserEmail, String mailbox) {
        Map<String, Mailbox> boxes = userMailboxes.computeIfAbsent(fullUserEmail, k -> new HashMap<>());

        return boxes.computeIfAbsent(mailbox, Mailbox::new);
    }

    // Called by SMTP when receiving a local email
    public synchronized  void storeEmailForAddress(Email email) {
        String to = email.getTo(); // Déjà normalisé

        to = to.trim();
        if (to.startsWith("<") && to.endsWith(">")) {
            to = to.substring(1, to.length() - 1);
        }
        to = to.toLowerCase(); // normalize to lowercase
        String[] parts = to.split("@");

        if (parts.length != 2) {
            System.err.println("Invalid email format: " + to);
            return;
        }

        String user = parts[0];
        String domain = parts[1];

        if (!domain.equals(localDomain)) {
            // Should not happen if SMTP handler checks domain correctly
            System.err.println("[STORAGE ERROR] Wrong domain: " + domain + " (expected " + localDomain + ")");
            return;
        }

        String fullUserEmail = user + "@" + domain;
        Mailbox inbox = getOrCreateMailbox(fullUserEmail, "INBOX");
        inbox.addEmail(email); // add to INBOX
    }

    // POP3 delete
    public synchronized  void deleteMessages(String fullUserEmail, List<Integer> indices1Based) {
        Mailbox inbox = getOrCreateMailbox(fullUserEmail, "INBOX");
        inbox.deleteByIndices(indices1Based);
    }

    // IMAP UIDVALIDITY
    public synchronized  long getUidValidity(String fullUserEmail, String mailboxName) {
        Mailbox inbox = getOrCreateMailbox(fullUserEmail, mailboxName);
        return inbox.getUidValidity();
    }

    // IMAP/POP3 get all emails from a specific mailbox
    public synchronized  List<Email> getEmailsForMailbox(String fullUserEmail, String mailboxName) {
        Mailbox box = getOrCreateMailbox(fullUserEmail, mailboxName);
        return box.getAllEmails();
    }

    // POP3 compatibility - always uses INBOX
    public synchronized  List<Email> getEmailsForUser(String fullUserEmail) {
        return getEmailsForMailbox(fullUserEmail, "INBOX");
    }

    // POP3 delete single
    public synchronized  void deleteEmailForUser(String fullUserEmail, int index1Based) {
        Mailbox inbox = getOrCreateMailbox(fullUserEmail, "INBOX");
        inbox.deleteByIndices(List.of(index1Based));
    }

    // Delete mailbox
    public synchronized boolean deleteMailbox(String user, String mailbox) {
        Map<String, Mailbox> boxes = userMailboxes.get(user);
        if (boxes == null)
            return false;
        return boxes.remove(mailbox) != null;
    }

    // Rename mailbox
    public synchronized boolean renameMailbox(String user, String oldName, String newName) {
        Map<String, Mailbox> boxes = userMailboxes.get(user);
        if (boxes == null)
            return false;

        Mailbox box = boxes.remove(oldName);
        if (box == null || boxes.containsKey(newName))
            return false;

        boxes.put(newName, box);
        return true;
    }

    // Copy email between mailboxes
    public synchronized boolean copyEmail(String user, String fromMailbox, String toMailbox, int index) {
        Map<String, Mailbox> boxes = userMailboxes.get(user);
        if (boxes == null)
            return false;

        Mailbox src = boxes.get(fromMailbox);
        Mailbox dst = boxes.get(toMailbox);

        if (src == null || dst == null)
            return false;

        List<Email> list = src.getAllEmails();
        if (index < 1 || index > list.size())
            return false;

        Email original = list.get(index - 1);

        // Create a copy of the email
        Email copy = new Email(
                original.getFrom(),
                original.getTo(),
                original.toRawMessage());

        dst.addEmail(copy); // add copy to destination mailbox
        return true;
    }

    // IMAP subscription methods
    private synchronized  Set<String> getSubscriptions(String user) {
        return subscriptions.computeIfAbsent(user, u -> {
            Set<String> s = new HashSet<>();
            s.add("INBOX");
            return s;
        });
    }

    // List mailboxes for user
    public synchronized  List<String> listMailboxesForUser(String user) {
        return new ArrayList<>(
                userMailboxes
                        .computeIfAbsent(user, u -> new HashMap<>())
                        .keySet());
    }

    // List subscribed mailboxes for user
    public synchronized List<String> listSubscribedMailboxes(String user) {
        return new ArrayList<>(getSubscriptions(user));
    }

    // Subscribe to mailbox
    public synchronized void subscribeMailbox(String user, String mailbox) {
        getSubscriptions(user).add(mailbox);
    }

    // Unsubscribe from mailbox
    public synchronized void unsubscribeMailbox(String user, String mailbox) {
        getSubscriptions(user).remove(mailbox);
    }

    // IMAP next UID
    public synchronized long getNextUid(String fullUserEmail, String mailboxName) {
        Mailbox box = getOrCreateMailbox(fullUserEmail, mailboxName);
        return box.getNextUid();
    }

    public synchronized boolean removeEmail(String fullUserEmail, String mailboxName, int index0Based) {
        Mailbox box = getOrCreateMailbox(fullUserEmail, mailboxName);
        return box.removeByIndex0Based(index0Based);
    }

    public synchronized boolean storeEmailFlags(String fullUserEmail, String mailboxName, int index1Based, Set<String> flags) {
        Mailbox box = getOrCreateMailbox(fullUserEmail, mailboxName);
        Email email = box.getByIndex(index1Based); // index1Based direct
        if (email == null)
            return false;

        email.getFlags().clear();
        email.getFlags().addAll(flags);
        return true;
    }

}


/**
 * Mailbox class representing a collection of emails.
 * author 
 *  -Dave Donkeng ndia
 *  -leslie lucynda tingue
 * version 1.0
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mailbox {

    private final String name; // e.g. INBOX
    private final List<Email> emails = new ArrayList<>();
    private final long uidValidity;
    private long nextUid = 1;

    /**
     * Constructor for Mailbox.
     * 
     * @param name mailbox name
     */
    public Mailbox(String name) {
        this.name = name;
        this.uidValidity = System.currentTimeMillis(); // simple UIDVALIDITY
    }

    /* get mailbox name */
    public String getName() {
        return name;
    }

    public synchronized long getNextUid() {
        return nextUid;
    }

    // Add email to mailbox
    public synchronized void addEmail(Email email) {
        email.setUid(nextUid++);
        emails.add(email);
    }

    // Return all emails
    public synchronized List<Email> getAllEmails() {
        return new ArrayList<>(emails);
    }

    // get email by 1-based index
    public synchronized Email getByIndex(int index1Based) {
        if (index1Based < 1 || index1Based > emails.size()) {
            return null;
        }
        return emails.get(index1Based - 1);
    }

    // delete emails by 1-based indices
    public synchronized void deleteByIndices(List<Integer> indices1Based) {
        // Sort descending to avoid shifting
        List<Integer> sorted = new ArrayList<>(indices1Based);
        sorted.sort(Collections.reverseOrder());

        for (int idx : sorted) {
            if (idx >= 1 && idx <= emails.size()) {
                emails.remove(idx - 1);
            }
        }
    }

    // delete email by 0-based index
    public synchronized boolean removeByIndex0Based(int index0Based) {
        if (index0Based < 0 || index0Based >= emails.size())
            return false;
        emails.remove(index0Based);
        return true;
    }

    // get UID validity
    public  long getUidValidity() {
        return uidValidity;
    }
}

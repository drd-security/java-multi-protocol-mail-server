
/**
 * Email class representing an email message.
 * Stores the raw SMTP message exactly as received (RFC 5321 compliant).
 * author
 *  - Dave Donkeng ndia
 *  - Leslie Lucynda Tingue
 * version 2.0
 */

import java.util.HashSet;
import java.util.Set;

public class Email {

    private final String from;
    private final String to;

    // RAW RFC822 / SMTP message (headers + body)
    private final String rawMessage;

    // Unique message UID (IMAP requirement)
    private long uid;

    // Message flags (\Seen, \Answered, \Flagged, \Deleted)
    private final Set<String> flags;

    /**
     * Constructor used by SMTP server.
     *
     * @param from       sender address
     * @param to         recipient address
     * @param rawMessage full SMTP DATA content (exact, unchanged)
     */
    public Email(String from, String to, String rawMessage) {
        this.from = from;
        this.to = to;
        this.rawMessage = rawMessage;
        this.flags = new HashSet<>();
    }

    /**
     * Get sender address.
     * 
     * @return
     */
    public String getFrom() {
        return from;
    }

    // Set unique message UID
    public void setUid(long uid) {
        this.uid = uid;
    }

    // Get recipient address
    public String getTo() {
        return to;
    }

    // Get unique message UID
    public long getUid() {
        return uid;
    }

    // get message flags
    public Set<String> getFlags() {
        return flags;
    }

    // convert flags to IMAP string
    public String flagsToImapString() {
        if (flags.isEmpty())
            return "";
        return String.join("", flags);
    }

    /**
     * Returns the message exactly as received via SMTP DATA.
     */
    public String toRawMessage() {
        return rawMessage;
    }

    // get size of raw message in octets
    public int getSize() {
        return rawMessage.length();
    }
}

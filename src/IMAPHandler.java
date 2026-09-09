
/**
 * IMAPHandler.java implements the IMAP protocol command handling.
 * author
 *  - Dave Donkeng ndia
 *  - Leslie Lucynda Tingue
 * version 1.0
 */
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IMAPHandler {

    private enum State {
        NONAUTH, AUTH, SELECTED
    }

    private State state = State.NONAUTH;
    private boolean logoutRequested = false;
    private String username;
    private String selectedMailbox = null;

    private String lastTag;

    public boolean isLogoutRequested() {
        return logoutRequested;
    }

    public String getLastTag() {
        return lastTag;
    }
    /**
     * Handles an IMAP command.
     * @param line The command line received.
     * @param out The PrintWriter to send responses.
     * @param storage The MailStorage instance for mailbox operations.
     * @param authenticator The UserAuthenticator for user authentication.
     */
    public void handleCommand(String line,
            PrintWriter out,
            MailStorage storage,
            UserAuthenticator authenticator) throws IMAPException {

        // Split the command line into tag, command, and arguments
        String[] parts = line.split(" ", 3);
        if (parts.length < 2) {
            throw new IMAPException("* BAD Invalid command format");
        }

        String tag = parts[0];
        String cmd = parts[1].toUpperCase();
        String args = (parts.length == 3) ? parts[2] : "";

        this.lastTag = tag;

        switch (cmd) {
            case "CAPABILITY":
                handleCapability(tag, out);
                break;

            case "NOOP":
                sendIMAPmessage(tag, out, "OK NOOP completed");
                break;

            case "LOGOUT":
                handleLogout(tag, out);
                break;

            case "LOGIN":
                requireState(State.NONAUTH, tag);
                handleLogin(tag, args, authenticator, out);
                break;

            case "LIST":
                requireAuth(tag);
                handleList(tag, out, storage);
                break;

            case "LSUB":
                requireAuth(tag);
                handleLsub(tag, out, storage);
                break;

            case "SELECT":
                requireAuth(tag);
                handleSelect(tag, args, out, storage);
                break;

            case "CREATE":
                requireAuth(tag);
                handleCreate(tag, args, out, storage);
                break;

            case "DELETE":
                requireAuth(tag);
                handleDelete(tag, args, out, storage);
                break;

            case "RENAME":
                requireAuth(tag);
                handleRename(tag, args, out, storage);
                break;

            case "SUBSCRIBE":
                requireAuth(tag);
                handleSubscribe(tag, args, out, storage);
                break;

            case "UNSUBSCRIBE":
                requireAuth(tag);
                handleUnsubscribe(tag, args, out, storage);
                break;

            case "CLOSE":
                requireSelected(tag);
                handleClose(tag, out, storage);
                break;

            case "EXPUNGE":
                requireSelected(tag);
                handleExpunge(tag, out, storage);
                break;

            case "UID":
                requireSelected(tag);
                handleUID(tag, args, out, storage);
                break;

            case "FETCH":
                requireSelected(tag);
                handleFetch(tag, args, out, storage);
                break;

            case "STORE":
                requireSelected(tag);
                handleStore(tag, args, out, storage);
                break;

            case "COPY":
                requireSelected(tag);
                handleCopy(tag, args, out, storage);
                break;
            
            case "APPEND":
                requireAuth(tag);
                handleAppend(tag, args, out, storage);
                sendIMAPmessage(tag, out, "OK APPEND completed");
                break;

            default:
                throw new IMAPException(tag + " BAD Unknown command");
        }
    }

    /**
     * Requires the current state to be the specified state.
     */
    private void requireState(State requiredState, String tag) throws IMAPException {
        if (state != requiredState)
            throw new IMAPException(tag + " BAD Invalid state");
    }

    /**
    Requires the current state to be AUTH
     */
    private void requireAuth(String tag) throws IMAPException {
        if (state != State.AUTH && state != State.SELECTED)
            throw new IMAPException(tag + " NO Must be authenticated");
    }

    /**
     * Requires the current state to be SELECTED.
     */
    private void requireSelected(String tag) throws IMAPException {
        if (state != State.SELECTED)
            throw new IMAPException(tag + " NO No mailbox selected");
    }

    private void handleCapability(String tag, PrintWriter out) {
        send(out, "* CAPABILITY IMAP4rev1 UID");
        sendIMAPmessage(tag, out, "OK CAPABILITY completed");
    }

    /**
     * Handles the LOGIN command.
     * @param tag The command tag.
     * @param args The command arguments.
     * @param auth The UserAuthenticator for authentication.
     * @param out The PrintWriter to send responses.
     */
    private void handleLogin(
            String tag,
            String args,
            UserAuthenticator auth,
            PrintWriter out) throws IMAPException {

        String[] token = args.split(" ");
        if (token.length < 2) {
            throw new IMAPException(tag + " BAD Missing arguments");
        }

        String user = strip(token[0]);
        String pass = strip(token[1]);
        String candidate;

        if (user.contains("@")) {
            candidate = user;
        } else {
            candidate = user + "@" + auth.getLocalDomain();
        }

        if (!candidate.endsWith("@" + auth.getLocalDomain())) {
            throw new IMAPException(tag + " NO BAD domain");
        }

        if (!auth.authenticate(candidate, pass)) {
            throw new IMAPException(tag + " NO Authentication failed");
        }

        username = candidate;
        state = State.AUTH;
        sendIMAPmessage(tag, out, "OK LOGIN completed");
    }

    private void handleLogout(String tag, PrintWriter out) {
        send(out, "* BYE IMAP server logging out");
        sendIMAPmessage(tag, out, "OK LOGOUT completed");
        logoutRequested = true;
    }
    /*liste mailboxes for user */
    private void handleList(String tag, PrintWriter out, MailStorage storage) {
        List<String> boxes = storage.listMailboxesForUser(username);
        if (boxes.isEmpty()) {
            send(out, "* LIST (\\HasNoChildren) \"/\" INBOX");
        } else {
            for (String b : boxes)
                send(out, "* LIST (\\HasNoChildren) \"/\" " + b);
        }
        sendIMAPmessage(tag, out, "OK LIST completed");
    }

    /*liste subscribed mailboxes for user */
    private void handleLsub(String tag, PrintWriter out, MailStorage storage) {
        List<String> boxes = storage.listSubscribedMailboxes(username);
        if (boxes.isEmpty()) {
            send(out, "* LSUB (\\HasNoChildren) \"/\" INBOX");
        } else {
            for (String b : boxes)
                send(out, "* LSUB (\\HasNoChildren) \"/\" " + b);
        }
        sendIMAPmessage(tag, out, "OK LSUB completed");
    }

    /*select a mailbox */   
    private void handleSelect(String tag, String mailbox,
            PrintWriter out,
            MailStorage storage) throws IMAPException {


        mailbox = strip(mailbox);
        mailbox = mailbox.equalsIgnoreCase("inbox") ? "INBOX" : mailbox;

        state = State.SELECTED;
        selectedMailbox = mailbox;

        Mailbox selectedBox = storage.getOrCreateMailbox(username, mailbox);
        List<Email> mails = selectedBox.getAllEmails();

        send(out, "* " + mails.size() + " EXISTS");
        sendIMAPmessage(tag, out, "OK [UIDVALIDITY " +
                storage.getUidValidity(username, mailbox) + "] SELECT completed");
    }

    private void handleCreate(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {

        String mailbox = strip(args);
        mailbox = mailbox.equalsIgnoreCase("inbox") ? "INBOX" : mailbox;

        if (mailbox.equalsIgnoreCase("INBOX")) {
            throw new IMAPException(tag + " BAD Cannot create INBOX");
        }

        storage.getOrCreateMailbox(username, mailbox);
        sendIMAPmessage(tag, out, "OK CREATE completed");
    }
    /*delete a mailbox */
    private void handleDelete(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {

        String mailbox = strip(args);
        mailbox = mailbox.equalsIgnoreCase("inbox") ? "INBOX" : mailbox;

        if (mailbox.equalsIgnoreCase("INBOX")) {
            throw new IMAPException(tag + " BAD Cannot delete INBOX");
        }
        storage.deleteMailbox(username, mailbox);
        sendIMAPmessage(tag, out, "OK DELETE completed");
    }

    private void handleRename(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {
        String[] p = args.split(" ", 2);
        if (p.length < 2) {
            throw new IMAPException(tag + " NO RENAME requires <old> <new>");
        }

        String oldName = strip(p[0]);
        String newName = strip(p[1]);

        if (oldName.equalsIgnoreCase("INBOX")) {
            throw new IMAPException(tag + " BAD Cannot rename INBOX");
        }

        storage.renameMailbox(username, oldName, newName);
        sendIMAPmessage(tag, out, "OK RENAME completed");
    }
    /*subscribe to a mailbox */
    private void handleSubscribe(String tag, String args,
            PrintWriter out, MailStorage storage) {
        String mailbox = strip(args);
        storage.subscribeMailbox(username, mailbox);
        sendIMAPmessage(tag, out, "OK SUBSCRIBE completed");
    }

    private void handleUnsubscribe(String tag, String args,
            PrintWriter out, MailStorage storage) {
        String mailbox = strip(args);
        mailbox = mailbox.equalsIgnoreCase("inbox") ? "INBOX" : mailbox;

        storage.unsubscribeMailbox(username, mailbox);
        sendIMAPmessage(tag, out, "OK UNSUBSCRIBE completed");
    }

    private void handleClose(String tag, PrintWriter out, MailStorage storage) {
        handleExpunge(tag, out, storage);
        state = State.AUTH;
        selectedMailbox = null;
        sendIMAPmessage(tag, out, "OK CLOSE completed");
    }

    /*expunge deleted emails */
    private void handleExpunge(String tag, PrintWriter out, MailStorage storage) {
        List<Email> emails = storage.getEmailsForMailbox(username, selectedMailbox);
        List<Integer> toDelete = new ArrayList<>();

        for (int i = 0; i < emails.size(); i++) {
            if (emails.get(i).getFlags().contains("\\Deleted")) {
                send(out, "* " + (i + 1) + " EXPUNGE");
                toDelete.add(i + 1);
            }
        }

        for (int index : toDelete) {
            storage.deleteEmailForUser(username, index);
        }

        sendIMAPmessage(tag, out, "OK EXPUNGE completed");
    }
    /**
     * Handles the UID command.
     * @param tag
     * @param args
     * @param out
     * @param storage
     */
    private void handleUID(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {

        String[] parts = args.split(" ", 2);
        if (parts.length < 2)
            throw new IMAPException(tag + " BAD UID requires subcommand");

        switch (parts[0].toUpperCase()) {
            case "FETCH":
                handleUidFetch(tag, parts[1], out, storage);
                break;
            case "STORE":
                handleUidStore(tag, parts[1], out, storage);
                break;
            case "COPY":
                handleUidCopy(tag, parts[1], out, storage);
                break;
            default:
                throw new IMAPException(tag + " BAD Unknown UID command");
        }
    }

    private void handleUidFetch(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {

         // extracte the range part
        String range;
        int parenIdx = args.indexOf('(');
        if (parenIdx > 0) {
            range = args.substring(0, parenIdx).trim();
        } else {
            String[] parts = args.split("\\s+", 2);
            range = parts[0];
        }


        List<Email> mails = storage.getEmailsForMailbox(username, selectedMailbox);

    
        // Handle different types of range
        if (range.equals("*")) {
            if (!mails.isEmpty()) {
                sendFetchResponse(out, mails.size(), mails.get(mails.size() - 1));
            }
        } else if (range.contains(":")) {
            String[] rangeParts = range.split(":");
            int start = Integer.parseInt(rangeParts[0]);
            int end;

            if (rangeParts[1].equals("*")) {
                end = mails.size();
            } else {
                end = Integer.parseInt(rangeParts[1]);
            }

            for (int i = start; i <= end && i <= mails.size(); i++) {
                sendFetchResponse(out, i, mails.get(i - 1));
            }
        } else {
            int index = parseIndex(range, mails.size());
            Email email = mails.get(index - 1);
            sendFetchResponse(out, index, email);
        }

        sendIMAPmessage(tag, out, "OK UID FETCH completed");
    }

    /**
     * Handles the UID STORE command 
     * @param tag
     * @param args
     * @param out
     * @param storage
     */
    private void handleUidStore(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {

        String[] parts = args.split(" ", 3);

        if (parts.length < 3)
            throw new IMAPException(tag + " BAD STORE requires <range> <item> <value>");

        int index = parseIndex(parts[0], storage.getEmailsForMailbox(username, selectedMailbox).size());
        Set<String> flags = parseFlags(parts[2]);

        if (storage.storeEmailFlags(username, selectedMailbox, index, flags)) {
            sendIMAPmessage(tag, out, "OK UID STORE completed");
        } else {
            throw new IMAPException(tag + " NO UID STORE failed");
        }
    }

    private void handleUidCopy(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {
        String[] parts = args.split(" ", 2);
        if (parts.length < 2)
            throw new IMAPException(tag + " BAD UID COPY requires <range> <mailbox>");

        String mailbox = strip(parts[1]);
        mailbox = mailbox.equalsIgnoreCase("inbox") ? "INBOX" : mailbox;

        List<Email> emails = storage.getEmailsForMailbox(username, selectedMailbox);
        int index = parseIndex(parts[0], emails.size());

        if (storage.copyEmail(username, selectedMailbox, mailbox, index)) {
            sendIMAPmessage(tag, out, "OK UID COPY completed");
        } else {
            throw new IMAPException(tag + " NO UID COPY failed");
        }
    }

    /**
     * Handles the FETCH command for a given email.
     * @param tag
     * @param args
     * @param out
     * @param storage
     */ 
    private void handleFetch(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {
        String range = args.split(" ")[0];
        List<Email> mails = storage.getEmailsForUser(username);

        if (range.equals("*") || range.contains(":")) {
            for (int i = 0; i < mails.size(); i++) {
                sendFetchResponse(out, i + 1, mails.get(i));
            }
        } else {
            int index = parseIndex(range, mails.size());
            Email email = mails.get(index - 1);
            sendFetchResponse(out, index, email);
        }

        sendIMAPmessage(tag, out, "OK FETCH completed");
    }

    private void handleStore(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {
        String[] parts = args.split(" ", 3);

        if (parts.length < 3)
            throw new IMAPException(tag + " BAD STORE requires <range> <item> <value>");

        int index = parseIndex(parts[0], storage.getEmailsForMailbox(username, selectedMailbox).size());
        Set<String> flags = parseFlags(parts[2]);

        if (storage.storeEmailFlags(username, selectedMailbox, index, flags)) {
            sendIMAPmessage(tag, out, "OK STORE completed");
        } else {
            throw new IMAPException(tag + " NO STORE failed");
        }
    }

    private void handleCopy(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {
        String[] parts = args.split(" ", 2);
        if (parts.length < 2)
            throw new IMAPException(tag + " BAD COPY requires <range> <mailbox>");

        String mailbox = strip(parts[1]);
        mailbox = mailbox.equalsIgnoreCase("inbox") ? "INBOX" : mailbox;

        List<Email> emails = storage.getEmailsForMailbox(username, selectedMailbox);
        int index = parseIndex(parts[0], emails.size());

        if (storage.copyEmail(username, selectedMailbox, mailbox, index)) {
            sendIMAPmessage(tag, out, "OK COPY completed");
        } else {
            throw new IMAPException(tag + " NO COPY failed");
        }
    }

    /**
     * Handles the APPEND command To add a new email to a mailbox.
     * @param tag
     * @param args
     * @param out
     * @param storage
     */
    private void handleAppend(String tag, String args,
            PrintWriter out, MailStorage storage) throws IMAPException {

        String[] parts = args.split(" ", 3);
        if (parts.length < 3)
            throw new IMAPException(tag + " BAD APPEND");

        String mailbox = strip(parts[0]);
        mailbox = mailbox.equalsIgnoreCase("inbox") ? "INBOX" : mailbox;

        Set<String> flags = parseFlags(parts[1]);
        String message = parts[2];

        Email email = new Email(message, "", "");

        Mailbox box = storage.getOrCreateMailbox(username, mailbox);
        box.addEmail(email);
    }

    public void sendIMAPmessage(String tag, PrintWriter out, String msg) {
        out.print(tag + " " + msg + "\r\n");
        out.flush();
    }

    public void send(PrintWriter out, String msg) {
        out.print(msg + "\r\n");
        out.flush();
    }

    private String strip(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1)
            return s.substring(1, s.length() - 1);
        return s;
    }

    /**
     * Sends a FETCH response for a given email.
     * @param out The PrintWriter to send the response to.
     * @param idx The index of the email.
     * @param e The email to send.
     */
    private void sendFetchResponse(PrintWriter out, int idx, Email e) {
        String body = e.toRawMessage();
        int size = body.length();
        String flags = e.flagsToImapString();

        out.print("* " + idx + " FETCH (UID " + e.getUid()
                + " FLAGS (" + flags + ")"
                + " RFC822.SIZE " + size
                + " BODY[] {" + size + "}\r\n");
        out.flush();

        out.print(body);

        if (!body.endsWith("\r\n")) {
            out.print("\r\n");
        }

        out.println(")");
        out.flush();

    }
    /**
     * Parses a message index from a string.
     * @param s The string to parse.
     * @param max The maximum valid index.
     * @return The parsed index.
     * @throws IMAPException If the index is invalid.
     */
    private int parseIndex(String s, int max) throws IMAPException {
        try {
            int v = Integer.parseInt(strip(s));
            if (v < 1 || v > max)
                throw new IMAPException("No such message: " + v);
            return v;
        } catch (NumberFormatException e) {
            throw new IMAPException("Invalid message number");
        }
    }

    /**
     * Parses a set of flags from a string.
     * @param flagStr The string containing flags.
     * @return A set of flags.
     */
    private Set<String> parseFlags(String flagStr) {
        Set<String> flags = new HashSet<>();
        flagStr = strip(flagStr);

        if (flagStr.startsWith("(") && flagStr.endsWith(")")) {
            flagStr = flagStr.substring(1, flagStr.length() - 1);
        }

        String[] parts = flagStr.split("\\s+");
        for (String flag : parts) {
            flag = flag.trim();
            if (!flag.isEmpty()) {
                flags.add(flag);
            }
        }

        return flags;
    }
}
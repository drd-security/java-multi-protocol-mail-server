
/**
 * SMTPHandler.java
 * Handles SMTP commands from a client and manages email storage and forwarding.
 * Author:
 *  - dave ronic donkeng
 *  - leslie lucynda tingue
 * Version: 1.0
 */
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SMTPHandler {

    private String heloDomain;
    private String mailFrom;
    private final List<String> rcptTo = new ArrayList<>();
    private boolean dataMode = false;
    private final List<String> dataLines = new ArrayList<>();
    private boolean quitRequested = false;

    /**
     * Handles an incoming SMTP command line.
     * * @param line command line
     * 
     * @param out         output writer
     * @param storage     mail storage
     * @param dnsResolver dns resolver
     * @param localDomain local domain
     * @throws SMTPException triggered on SMTP errors
     */
    public void handleCommand(
            String line,
            PrintWriter out,
            MailStorage storage,
            DNSResolver dnsResolver,
            String localDomain) throws SMTPException {

        // --- data mode ---
        if (dataMode) {
            handleDataMode(line, out, storage, dnsResolver, localDomain);
            return;
        }

        // --- parsing commands ---
        String[] parts = line.split(" ", 2);
        String cmd = parts[0].toUpperCase();
        String args = (parts.length > 1) ? parts[1] : "";

        switch (cmd) {
            case "EHLO":
                handleEhlo(args, out, localDomain);
                break;
            case "HELO":
                handleHello(args, out, localDomain);
                break;
            case "MAIL":
                handleMailFrom(args, out);
                break;
            case "RCPT":
                handleRcptTo(args, out);
                break;
            case "DATA":
                handleData(out);
                break;
            case "QUIT":
                handleQuit(out);
                break;
            default:
                // Send 500 for unrecognized command
                sendSMTPmessage(out, "500 Syntax error, unrecognized command");
        }
    }

    /**
     * Handles lines received in DATA mode.
     * 
     * @param line        line received
     * @param out         output writer
     * @param storage     mail storage
     * @param dnsResolver dns resolver
     * @param localDomain local domain
     */
    private void handleDataMode(
            String line,
            PrintWriter out,
            MailStorage storage,
            DNSResolver dnsResolver,
            String localDomain) {

        // End of DATA
        if (line.equals(".")) {
            dataMode = false;
            String from = extractAddress(mailFrom);

            if (from == null) {
                sendSMTPmessage(out, "550 Invalid sender address: " + mailFrom);
                resetMessageState();
                return;
            }

            // reconstruct raw message
            String rawMessage = String.join("\r\n", dataLines) + "\r\n";
            for (String to : rcptTo) {

                Email email = new Email(from, to, rawMessage); // create email object

                String destDomain = getDomainFromAddress(to);
                if (destDomain.equalsIgnoreCase(localDomain)) {
                    storage.storeEmailForAddress(email); // local delivery
                } else {
                    forwardEmail(email, destDomain, dnsResolver, localDomain); // dns lookup + forward
                }
            }

            sendSMTPmessage(out, "250 OK Message accepted for delivery");
            resetMessageState(); // reset state for next message
            return;
        }

        // Accumulate data lines
        dataLines.add(line);
    }

    // -- smtp command handlers --
    /**
     * Handles the EHLO command.
     * 
     * @param args        command arguments
     * @param out         output writer
     * @param localDomain local domain
     * @throws SMTPException triggered on syntax errors
     */
    private void handleEhlo(String args, PrintWriter out, String localDomain) throws SMTPException {
        if (args == null || args.isEmpty()) {
            throw new SMTPException("501 Syntax: EHLO hostname");
        }
        heloDomain = args.trim();
        sendSMTPmessage(out, "250-" + localDomain + " greets " + heloDomain);
        sendSMTPmessage(out, "250-8BITMIME");
        sendSMTPmessage(out, "250 OK");
    }

    /**
     * Handles the HELO command.
     * 
     * @param args        command arguments
     * @param out         output writer
     * @param localDomain local domain
     * @throws SMTPException triggered on syntax errors
     */
    private void handleHello(String args, PrintWriter out, String localDomain) throws SMTPException {
        if (args == null || args.isEmpty()) {
            throw new SMTPException("Missing HELO domain");
        }
        heloDomain = args.trim();
        sendSMTPmessage(out, "250 " + localDomain + " greets " + heloDomain);
    }

    /**
     * Handles the MAIL FROM command.
     * 
     * @param args command arguments
     * @param out  output writer
     * @throws SMTPException triggered on syntax errors
     */
    private void handleMailFrom(String args, PrintWriter out) throws SMTPException {
        if (!args.toUpperCase().startsWith("FROM:")) {
            throw new SMTPException("Syntax: MAIL FROM:<address>");
        }
        // Extract address
        mailFrom = args.substring(5).trim();
        sendSMTPmessage(out, "250 OK");
    }

    /**
     * Handles the RCPT TO command.
     * 
     * @param args command arguments
     * @param out  output writer
     * @throws SMTPException triggered on syntax errors
     */
    private void handleRcptTo(String args, PrintWriter out) throws SMTPException {
        if (!args.toUpperCase().startsWith("TO:")) {
            throw new SMTPException("Syntax: RCPT TO:<address>");
        }

        String address = extractAddress(args.substring(3).trim());
        if (address == null) {
            throw new SMTPException("Invalid recipient address");
        }

        rcptTo.add(address); // add recipient
        sendSMTPmessage(out, "250 OK");
    }

    /**
     * Handles the DATA command.
     * 
     * @param out output writer
     * @throws SMTPException triggered on syntax errors
     */
    private void handleData(PrintWriter out) throws SMTPException {
        if (mailFrom == null || rcptTo == null) {
            throw new SMTPException("MAIL FROM and RCPT TO required");
        }
        dataMode = true;
        sendSMTPmessage(out, "354 End data with <CRLF>.<CRLF>");
    }

    /**
     * Handles the QUIT command.
     * 
     * @param out output writer
     */
    private void handleQuit(PrintWriter out) {
        sendSMTPmessage(out, "221 Bye");
        quitRequested = true;
    }

    /**
     * Forwards an email to the specified destination domain using DNS resolution.
     * 
     * @param email       email to forward
     * @param destDomain  destination domain
     * @param dnsResolver DNS resolver for MX lookups
     * @param localDomain local domain
     */

    private void forwardEmail(
            Email email,
            String destDomain,
            DNSResolver dnsResolver,
            String localDomain) {

        String ip = dnsResolver.resolveMailServerIp(destDomain);
        if (ip == null) {
            System.err.println("SMTP forward failed: " + destDomain);
            return;
        }

        try (Socket socket = new Socket(ip, 25);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true)) {

            readReply(in); // 220

            send(out, in, "HELO " + localDomain);
            send(out, in, "MAIL FROM:<" + email.getFrom() + ">");
            send(out, in, "RCPT TO:<" + email.getTo() + ">");
            send(out, in, "DATA");

            // Send raw message
            out.print(email.toRawMessage());
            if (!email.toRawMessage().endsWith("\r\n")) {
                out.print("\r\n");
            }
            out.print(".\r\n");
            out.flush();

            readReply(in); // 250
            send(out, in, "QUIT");

        } catch (IOException e) {
            System.err.println("SMTP forward failed: " + e.getMessage());
        }
    }

    /**
     * Extracts the domain part from an email address.
     * 
     * @param addr email address
     * @return domain part or null if invalid
     */
    private String getDomainFromAddress(String addr) {
        if (addr == null)
            return null;
        int at = addr.lastIndexOf('@');
        if (at < 0 || at == addr.length() - 1)
            return null;
        return addr.substring(at + 1);
    }

    /**
     * Sends a command and reads the reply.
     * 
     * @param out output writer
     * @param in  input reader
     * @param cmd command to send
     * @throws IOException triggered on I/O errors
     */
    private void send(PrintWriter out, BufferedReader in, String cmd)
            throws IOException {
        out.print(cmd + "\r\n");
        out.flush();
        readReply(in);
    }

    /**
     * Reads the SMTP reply from the server.
     * 
     * @param in input reader
     * @throws IOException triggered on I/O errors
     */
    private void readReply(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            // SMTP multi-line
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break;
            }
        }
    }

    /**
     * Extracts the email address from a raw string.
     * 
     * @param raw raw string containing email address
     * @return extracted email address or null if invalid
     */
    private String extractAddress(String raw) {
        if (raw == null)
            return null;

        int start = raw.indexOf('<');
        int end = raw.indexOf('>');

        if (start != -1 && end != -1 && end > start) {
            return raw.substring(start + 1, end);
        }

        // Fallback: split by space and take the first token
        return raw.trim().split("\\s+")[0];
    }

    /**
     * Resets the state for the next email message.
     */
    private void resetMessageState() {
        mailFrom = null;
        rcptTo.clear();
        dataLines.clear();
        dataMode = false;
    }

    /**
     * Checks if the QUIT command was requested.
     * 
     * @return true if QUIT was requested, false otherwise
     */
    public boolean isQuitRequested() {
        return quitRequested;
    }

    /**
     * Sends an SMTP message to the client.
     * 
     * @param out     output writer
     * @param message message to send
     */
    public void sendSMTPmessage(PrintWriter out, String message) {
        out.print(message + "\r\n");
        out.flush();
    }

}
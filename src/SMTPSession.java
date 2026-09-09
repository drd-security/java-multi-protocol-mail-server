import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SMTPSession implements Runnable {

    private final String localDomain;
    private final Socket socket;
    private final MailStorage storage;
    private final DNSResolver dnsResolver;

    /**
     * Constructor for SMTPSession.
     * 
     * @param localDomain server domain name
     * @param socket      client socket
     * @param storage     mail storage system
     * @param dnsResolver DNS resolver for MX lookups
     */
    public SMTPSession(String localDomain, Socket socket,
            MailStorage storage, DNSResolver dnsResolver) {
        this.localDomain = localDomain;
        this.socket = socket;
        this.storage = storage;
        this.dnsResolver = dnsResolver;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            SMTPHandler handler = new SMTPHandler();
            // Greeting banner
            handler.sendSMTPmessage(out, "220 " + localDomain + " Service ready");
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    // Handle SMTP command
                    handler.handleCommand(line, out, storage, dnsResolver, localDomain);
                    // Check for QUIT command
                    if (handler.isQuitRequested()) {
                        break;
                    }
                } catch (SMTPException e) {
                    handler.sendSMTPmessage(out, "501 " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("SMTP session error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }
}

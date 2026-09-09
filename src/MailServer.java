
/**
 * MailServer.java
 * Main class for the mail server application
 * Handles initialization and startup of SMTP, POP3, and IMAP servers
 * Author:
    * - dave ronic donkeng
    * - leslie lucynda tingue
 * Version: 1.0
 */
import java.io.IOException;

public class MailServer {
    private final String domainName;
    private final int maxThreads; // Maximum number of threads for handling client connections

    private final ServerThreadPool threadPool; // Thread pool for managing client connections
    private final MailStorage mailStorage; // Storage system for emails
    private final UserAuthenticator authenticator; // User authentication system
    private final DNSResolver dnsResolver; // DNS resolver for domain name lookups

    private SMTPServer smtpServer; // SMTP server instance
    private POP3Server pop3Server; // POP3 server instance
    private IMAPServer imapServer; // IMAP server instance

    public MailServer(String domainName, int maxThreads) {
        this.domainName = domainName;
        this.maxThreads = maxThreads;

        this.threadPool = new ServerThreadPool(maxThreads);
        this.mailStorage = new MailStorage(domainName);
        this.authenticator = new UserAuthenticator(domainName);
        this.dnsResolver = new DNSResolver();
    }

    public void start() throws IOException {
        int smtpPort = 25;
        int pop3Port = 110;
        int imapPort = 143;

        smtpServer = new SMTPServer(domainName, smtpPort, mailStorage, dnsResolver, threadPool);
        pop3Server = new POP3Server(domainName, pop3Port, mailStorage, authenticator, threadPool);
        imapServer = new IMAPServer(domainName, imapPort, mailStorage, authenticator, threadPool);

        // Start each server in its own thread
        new Thread(() -> {
            try {
                smtpServer.start();
            } catch (IOException e) {
                System.err.println("SMTP server stopped: " + e.getMessage());

            }
        }, "SMTP-Listener").start();

        new Thread(() -> {
            try {
                pop3Server.start();
            } catch (IOException e) {
                System.err.println("POP3 server stopped: " + e.getMessage());
            }
        }, "POP3-Listener").start();

        new Thread(() -> {
            try {
                imapServer.start();
            } catch (IOException e) {
                System.err.println("IMAP server stopped: " + e.getMessage());
            }
        }, "IMAP-Listener").start();

        System.out.println("MailServer for domain " + domainName +
                " started with maxThreads=" + maxThreads);
    }

    /**
     * Shuts down the mail server and releases resources.
     */
    public void shutdown() {
        System.out.println("Shutting down server...");
        try {
            if (smtpServer != null)
                smtpServer.stop();
            if (pop3Server != null)
                pop3Server.stop();
            if (imapServer != null)
                imapServer.stop();
        } catch (IOException e) {
            System.err.println("Error while closing servers: " + e.getMessage());
        }
        threadPool.shutdown();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java MailServer <domain> <maxThreads>");
            System.exit(1);
        }

        String domain = args[0];
        int maxThreads = Integer.parseInt(args[1]);

        MailServer server = new MailServer(domain, maxThreads);
        try {
            server.start();
        } catch (IOException e) {
            server.shutdown();
            e.printStackTrace();
        }
    }

}
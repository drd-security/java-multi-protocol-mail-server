
/**
 * SMTPServer.java
 * SMTP server implementation to handle incoming email delivery.
 * Author:
 *  - dave ronic donkeng
 *  - leslie lucynda tingue
 * Version: 1.0
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SMTPServer {
    private final String domain;
    private final int port;
    private final MailStorage storage;
    private final DNSResolver dnsResolver;
    private final ServerThreadPool threadPool;

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    /**
     * Constructor for SMTPServer.
     * 
     * @param domain      server domain name
     * @param port        listening port
     * @param storage     mail storage system
     * @param dnsResolver DNS resolver for MX lookups
     * @param threadPool  thread pool for handling connections
     */
    public SMTPServer(String domain, int port, MailStorage storage, DNSResolver dnsResolver,
            ServerThreadPool threadPool) {
        this.domain = domain;
        this.port = port;
        this.storage = storage;
        this.dnsResolver = dnsResolver;
        this.threadPool = threadPool;
    }

    /**
     * Starts the SMTP server to listen for incoming connections.
     * 
     * @throws IOException on socket errors
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("SMTP server listening on port " + port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                SMTPSession session = new SMTPSession(domain, clientSocket, storage, dnsResolver);
                threadPool.execute(session);

            } catch (IOException e) {
                if (running) {
                    System.err.println("SMTP accept error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Stops the SMTP server.
     * 
     * @throws IOException on socket errors
     */
    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}

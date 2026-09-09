
/**
 * POP3Server is responsible for handling incoming POP3 connections,
 * managing sessions, and coordinating with the mail storage and user
 * authentication components.
 * 
 * Authors:
 * - dave donkeng ndia
 * - leslie lucynda tingue
 * Version: 1.0
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class POP3Server {

    private final String domain;
    private final int port;
    private final MailStorage storage;
    private final UserAuthenticator authenticator;
    private final ServerThreadPool threadPool;

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    /**
     * Constructor for POP3Server.
     * 
     * @param domain        server domain name
     * @param port          listening port
     * @param threadPool    thread pool for handling connections
     * @param storage       mail storage system
     * @param authenticator user authentication system
     */
    public POP3Server(String domain, int port, MailStorage storage,
            UserAuthenticator authenticator, ServerThreadPool threadPool) {

        this.domain = domain;
        this.port = port;
        this.storage = storage;
        this.authenticator = authenticator;
        this.threadPool = threadPool;
    }

    /**
     * Starts the POP3 server to listen for incoming connections.
     * 
     * @throws IOException on socket errors
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        System.out.println("POP3 server listening on port " + port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                POP3Session session = new POP3Session(domain, clientSocket, storage, authenticator);
                threadPool.execute(session);
            } catch (IOException e) {
                if (running) {
                    System.err.println("POP3 accept error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Stops the POP3 server.
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

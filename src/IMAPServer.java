
/**
 * IMAPServer listens for incoming IMAP connections,
 * spawning IMAPSession instances to handle each client.
 * 
 * Authors:
 * - dave donkeng ndia
 * - leslie lucynda tingue
 * Version: 1.0
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class IMAPServer {

    private final String domain;
    private final int port;
    private final MailStorage storage;
    private final UserAuthenticator authenticator;
    private final ServerThreadPool threadPool;

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    /**
     * Constructor for IMAPServer.
     * 
     * @param domain        server domain name
     * @param port          listening port
     * @param storage       mail storage system
     * @param authenticator user authentication system
     * @param pool          thread pool for handling connections
     */
    public IMAPServer(String domain, int port,
            MailStorage storage,
            UserAuthenticator authenticator,
            ServerThreadPool pool) {

        this.domain = domain;
        this.port = port;
        this.storage = storage;
        this.authenticator = authenticator;
        this.threadPool = pool;
    }

    /**
     * Starts the IMAP server to listen for incoming connections.
     * 
     * @throws IOException on socket errors
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        System.out.println("IMAP server listening on port " + port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();

                IMAPSession session = new IMAPSession(domain, clientSocket, storage, authenticator);

                threadPool.execute(session);

            } catch (IOException e) {
                if (running) {
                    System.err.println("IMAP accept error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Stops the IMAP server.
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

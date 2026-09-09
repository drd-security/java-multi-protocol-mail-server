
/**
 * POP3Session handles an individual POP3 session with a client.
 * It processes commands and interacts with the mail storage and
 * user authentication components.
 * 
 * Authors:
 * - dave donkeng ndia
 * - leslie lucynda tingue
 * Version: 1.0
 */
import java.io.*;
import java.net.Socket;

public class POP3Session implements Runnable {

    private final String domain;
    private final Socket socket;
    private final MailStorage storage;
    private final UserAuthenticator authenticator;

    /**
     * Constructor for POP3Session.
     * 
     * @param domain        server domain name
     * @param socket        client socket
     * @param storage       mail storage system
     * @param authenticator user authentication system
     */
    public POP3Session(String domain, Socket socket,
            MailStorage storage,
            UserAuthenticator authenticator) {
        this.domain = domain;
        this.socket = socket;
        this.storage = storage;
        this.authenticator = authenticator;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            POP3Handler handler = new POP3Handler();

            // Greeting POP3
            handler.sendPOP3message(out, "+OK POP3 Service Ready");
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    handler.handleCommand(line, out, storage, authenticator);

                    if (handler.isQuitRequested()) {
                        break;
                    }

                } catch (POP3Exception e) {
                    handler.sendPOP3message(out, "-ERR " + e.getMessage());

                }
            }

        } catch (IOException e) {
            System.err.println("POP3 session error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }
}

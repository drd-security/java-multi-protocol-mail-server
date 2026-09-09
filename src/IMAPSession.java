
/**
 * IMAPSession manages an individual IMAP session with a client,
 * handling command processing, authentication, and session termination.
 * 
 * Authors:
 * - dave donkeng ndia
 * - leslie lucynda tingue
 * Version: 1.0
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class IMAPSession implements Runnable {

    private final String domain;
    private final Socket socket;
    private final MailStorage storage;
    private final UserAuthenticator authenticator;

    /**
     * Constructor for IMAPSession.
     * 
     * @param domain        server domain name
     * @param socket        client socket
     * @param storage       mail storage system
     * @param authenticator user authentication system
     */
    public IMAPSession(String domain, Socket socket,
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

            IMAPHandler handler = new IMAPHandler();

            // IMAP greeting
            handler.send(out, "* OK IMAP4rev1 Service Ready");

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    handler.handleCommand(line, out, storage, authenticator);

                    if (handler.isLogoutRequested()) {
                        break;
                    }

                } catch (IMAPException e) {
                    handler.send(out, "* BAD " + e.getMessage());
                }

            }

        } catch (IOException e) {
            System.err.println("IMAP session error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }
}

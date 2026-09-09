/**
 * Custom exception class for SMTP-related errors.
 * authors
 * - dave donkeng ndia
 * - leslie lucynda tingue
 * version 1.0
 */
public class SMTPException extends Exception {
    public SMTPException() {
        super();
    }

    public SMTPException(String message) {
        super(message);
    }

}

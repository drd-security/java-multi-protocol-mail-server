/**
 * Custom exception class for IMAP-related errors.
 * authors -dave donkeng ndia
 * - leslie lucynda tingue
 * version 1.0
 */
public class IMAPException extends Exception {
    public IMAPException() {
        super();
    }

    public IMAPException(String message) {
        super(message);
    }

}

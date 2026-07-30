package org.superquinquin.brevo;

public class BrevoException extends RuntimeException {

    public BrevoException(String message) {
        super(message);
    }

    public BrevoException(String message, Throwable cause) {
        super(message, cause);
    }
}

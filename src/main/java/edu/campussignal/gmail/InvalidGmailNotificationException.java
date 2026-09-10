package edu.campussignal.gmail;

public class InvalidGmailNotificationException extends IllegalArgumentException {
    public InvalidGmailNotificationException(String message) {
        super(message);
    }

    public InvalidGmailNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

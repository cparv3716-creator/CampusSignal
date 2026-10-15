package edu.campussignal.gmail;

import java.io.IOException;

/** Gmail no longer retains history for the persisted start history ID. */
public class StaleGmailHistoryException extends IOException {
    public StaleGmailHistoryException() {
        super("Stored Gmail history is no longer available");
    }
}

package edu.campussignal.service;

import java.io.IOException;
import java.util.List;
import edu.campussignal.dto.IncomingEmail;

public interface EmailSource {
    List<String> listMessageIds(int maxResults) throws IOException;
    IncomingEmail fetch(String messageId) throws IOException;
}

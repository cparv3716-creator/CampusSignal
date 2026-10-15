package edu.campussignal.service;

import edu.campussignal.dto.EmailClassificationResult;

public interface EmailClassificationService {
    EmailClassificationResult classify(String subject, String body);
}

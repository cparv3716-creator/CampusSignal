package edu.campussignal.service;

import java.io.IOException;

@FunctionalInterface
public interface LocalOllamaClient {
    String summarize(String prompt) throws IOException, InterruptedException;
}

package edu.campussignal.dto;

public record RetrievalSummary(int fetched, int created, int alreadyExisted, int failed) {
}

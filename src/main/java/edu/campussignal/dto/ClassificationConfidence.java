package edu.campussignal.dto;

public enum ClassificationConfidence {
    HIGH(0.9),
    MEDIUM(0.7),
    LOW(0.5);

    private final double persistedValue;

    ClassificationConfidence(double persistedValue) {
        this.persistedValue = persistedValue;
    }

    public double persistedValue() {
        return persistedValue;
    }

    public static ClassificationConfidence fromPersistedValue(double value) {
        for (var confidence : values()) {
            if (Double.compare(confidence.persistedValue, value) == 0) {
                return confidence;
            }
        }
        throw new IllegalArgumentException("Unsupported classification confidence value");
    }
}

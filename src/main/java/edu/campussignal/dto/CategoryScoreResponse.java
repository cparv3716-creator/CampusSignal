package edu.campussignal.dto;

public record CategoryScoreResponse(String category, double score, Components components) {
    public record Components(double explicitPreference, double profileRelevance,
                             double behavioralPreference, double relatedInterestSimilarity) {
        public Components {
            for (double value : new double[]{explicitPreference, profileRelevance,
                    behavioralPreference, relatedInterestSimilarity}) {
                if (!Double.isFinite(value) || value < 0 || value > 1) {
                    throw new IllegalArgumentException("Score components must be finite values in [0,1]");
                }
            }
        }
    }
}

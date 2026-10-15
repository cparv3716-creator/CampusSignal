package edu.campussignal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("category-score")
public record CategoryScoreProperties(
        @DefaultValue("0.40") double explicitWeight,
        @DefaultValue("0.15") double profileWeight,
        @DefaultValue("0.35") double behavioralWeight,
        @DefaultValue("0.10") double relatedWeight,
        @DefaultValue("0.5") double initialBehavioral,
        @DefaultValue("0.0") double initialRelated) {
    public CategoryScoreProperties {
        for (double value : new double[]{explicitWeight, profileWeight, behavioralWeight,
                relatedWeight, initialBehavioral, initialRelated}) {
            if (!Double.isFinite(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("Category score settings must be finite values in [0,1]");
            }
        }
        if (Math.abs(explicitWeight + profileWeight + behavioralWeight + relatedWeight - 1) > 1e-9) {
            throw new IllegalArgumentException("Category score weights must sum to 1");
        }
    }
}

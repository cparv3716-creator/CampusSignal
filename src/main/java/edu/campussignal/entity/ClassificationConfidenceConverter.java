package edu.campussignal.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import edu.campussignal.dto.ClassificationConfidence;

@Converter
public class ClassificationConfidenceConverter
        implements AttributeConverter<ClassificationConfidence, Double> {

    @Override
    public Double convertToDatabaseColumn(ClassificationConfidence confidence) {
        return confidence == null ? null : confidence.persistedValue();
    }

    @Override
    public ClassificationConfidence convertToEntityAttribute(Double value) {
        return value == null ? null : ClassificationConfidence.fromPersistedValue(value);
    }
}

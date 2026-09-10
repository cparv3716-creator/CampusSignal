package edu.campussignal.dto;

import java.util.List;
import java.util.Locale;
import jakarta.validation.constraints.*;

public record ProfileCreateRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 255) String department,
        @NotBlank @Size(max = 255) String programme,
        @NotNull @Min(1) Integer year,
        @Min(1) Integer semester,
        @NotEmpty List<@NotBlank @Size(max = 255) String> interests) {
    public ProfileCreateRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        department = department == null ? null : department.strip();
        programme = programme == null ? null : programme.strip();
        // Preserve invalid null/blank entries for Bean Validation to reject.
        interests = interests == null ? null : interests.stream()
                .map(value -> value == null ? null : value.strip().toLowerCase(Locale.ROOT))
                .distinct().toList();
    }
}

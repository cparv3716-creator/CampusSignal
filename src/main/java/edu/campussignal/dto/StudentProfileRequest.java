package edu.campussignal.dto;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record StudentProfileRequest(
        @NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 255) String college,
        @NotBlank @Size(max = 255) String department, @NotBlank @Size(max = 255) String programme,
        @NotNull @Min(1) Integer year, @NotNull @Min(2000) @Max(2200) Integer graduationYear,
        @Min(1) Integer semester, @NotEmpty List<@NotBlank @Size(max = 255) String> interests) {
    public StudentProfileRequest { interests = interests == null ? null : interests.stream().map(String::strip).filter(value -> !value.isBlank()).distinct().toList(); }
}

package edu.campussignal.dto;

import java.time.Instant;
import java.util.List;
import edu.campussignal.entity.UserProfile;

public record ProfileResponse(Long id, String email, String department, String programme,
                              int year, Integer semester, List<String> interests, Instant createdAt) {
    public ProfileResponse {
        interests = List.copyOf(interests);
    }

    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(profile.getId(), profile.getEmail(), profile.getDepartment(),
                profile.getProgramme(), profile.getYear(), profile.getSemester(),
                profile.getInterests(), profile.getCreatedAt());
    }
}

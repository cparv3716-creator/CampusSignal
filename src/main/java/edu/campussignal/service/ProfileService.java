package edu.campussignal.service;

import java.util.Locale;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import edu.campussignal.dto.ProfileCreateRequest;
import edu.campussignal.dto.ProfileResponse;
import edu.campussignal.entity.UserProfile;
import edu.campussignal.repository.UserProfileRepository;

@Service
@Validated
@Transactional(readOnly = true)
public class ProfileService {
    private final UserProfileRepository repository;

    public ProfileService(UserProfileRepository repository) { this.repository = repository; }

    @Transactional
    public ProfileResponse create(@Valid ProfileCreateRequest request) {
        return ProfileResponse.from(repository.saveAndFlush(new UserProfile(request)));
    }

    public ProfileResponse get(long id) {
        return ProfileResponse.from(repository.findById(id).orElseThrow(ProfileNotFoundException::new));
    }

    public ProfileResponse getByEmail(String email) {
        return ProfileResponse.from(repository.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(ProfileNotFoundException::new));
    }
}

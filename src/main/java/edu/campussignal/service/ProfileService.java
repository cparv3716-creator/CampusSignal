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
import edu.campussignal.entity.UserAccount;
import edu.campussignal.dto.StudentProfileRequest;
import edu.campussignal.dto.StudentProfileResponse;

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

    @Transactional
    public ProfileResponse update(long id, @Valid ProfileCreateRequest request) {
        UserProfile profile = repository.findById(id).orElseThrow(ProfileNotFoundException::new);
        profile.updateFrom(request);
        return ProfileResponse.from(repository.saveAndFlush(profile));
    }

    public ProfileResponse get(long id) {
        return ProfileResponse.from(repository.findById(id).orElseThrow(ProfileNotFoundException::new));
    }

    public ProfileResponse getByEmail(String email) {
        return ProfileResponse.from(repository.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(ProfileNotFoundException::new));
    }

    @Transactional
    public StudentProfileResponse saveForUser(UserAccount user, @Valid StudentProfileRequest request) {
        var profile = repository.findByUserId(user.getId()).orElseGet(() -> new UserProfile(user, request));
        if (profile.getId() != null) profile.updateFrom(request);
        return StudentProfileResponse.from(repository.saveAndFlush(profile));
    }

    public StudentProfileResponse getForUser(UserAccount user) {
        return StudentProfileResponse.from(repository.findByUserId(user.getId()).orElseThrow(ProfileNotFoundException::new));
    }
    public UserProfile repositoryForOwner(UserAccount user) { return repository.findByUserId(user.getId()).orElseThrow(ProfileNotFoundException::new); }
}

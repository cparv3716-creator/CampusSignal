package edu.campussignal.controller;

import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import edu.campussignal.dto.*;
import edu.campussignal.service.CategoryScoreService;
import edu.campussignal.service.ProfileService;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {
    private final ProfileService profiles;
    private final CategoryScoreService scores;

    public ProfileController(ProfileService profiles, CategoryScoreService scores) {
        this.profiles = profiles;
        this.scores = scores;
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> create(@Valid @RequestBody ProfileCreateRequest request) {
        var profile = profiles.create(request);
        return ResponseEntity.created(URI.create("/api/v1/profiles/" + profile.id())).body(profile);
    }

    @GetMapping("/{id}")
    public ProfileResponse get(@PathVariable long id) { return profiles.get(id); }

    @GetMapping("/by-email/{email}")
    public ProfileResponse getByEmail(@PathVariable String email) { return profiles.getByEmail(email); }

    @GetMapping("/{id}/category-score")
    public CategoryScoreResponse score(@PathVariable long id, @RequestParam String category) {
        return scores.score(profiles.get(id), category);
    }
}

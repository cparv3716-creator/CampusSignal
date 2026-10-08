package edu.campussignal.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.campussignal.dto.ProfileActionFeedItemResponse;
import edu.campussignal.service.ProfileActionFeedService;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileActionFeedController {
    private final ProfileActionFeedService feeds;

    public ProfileActionFeedController(ProfileActionFeedService feeds) {
        this.feeds = feeds;
    }

    @GetMapping("/{profileId}/action-feed")
    public List<ProfileActionFeedItemResponse> feed(@PathVariable long profileId) {
        return feeds.feed(profileId);
    }
}

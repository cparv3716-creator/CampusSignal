package edu.campussignal.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.campussignal.dto.ProfileActionFeedItemResponse;
import edu.campussignal.dto.RankedEmailFeedItemResponse;
import edu.campussignal.service.ProfileActionFeedService;
import edu.campussignal.service.ProfileEmailFeedService;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileActionFeedController {
    private final ProfileActionFeedService feeds;
    private final ProfileEmailFeedService emailFeeds;

    public ProfileActionFeedController(
            ProfileActionFeedService feeds, ProfileEmailFeedService emailFeeds) {
        this.feeds = feeds;
        this.emailFeeds = emailFeeds;
    }

    @GetMapping("/{profileId}/action-feed")
    public List<ProfileActionFeedItemResponse> feed(@PathVariable long profileId) {
        return feeds.feed(profileId);
    }

    @GetMapping("/{profileId}/email-feed")
    public List<RankedEmailFeedItemResponse> emailFeed(@PathVariable long profileId) {
        return emailFeeds.feed(profileId);
    }
}

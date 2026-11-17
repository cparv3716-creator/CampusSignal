package edu.campussignal.dto;

import java.util.List;

/** A non-persistent fixture workspace, returned only to the authenticated demo session. */
public record DemoWorkspaceResponse(
        boolean demoMode,
        StudentProfileResponse profile,
        List<EmailResponse> emails,
        List<RankedEmailFeedItemResponse> feed,
        List<EmailActionReadResponse> actions) {}

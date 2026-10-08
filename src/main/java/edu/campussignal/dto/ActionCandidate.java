package edu.campussignal.dto;

import java.time.LocalDate;

public record ActionCandidate(
        EmailActionType actionType,
        LocalDate deadline,
        String actionUrl) {}

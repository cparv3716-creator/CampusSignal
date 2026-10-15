package edu.campussignal.dto;

import jakarta.validation.constraints.NotNull;

public record EmailActionStatusRequest(@NotNull EmailActionStatus status) {}

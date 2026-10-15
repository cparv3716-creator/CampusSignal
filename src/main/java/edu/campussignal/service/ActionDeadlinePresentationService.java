package edu.campussignal.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import edu.campussignal.dto.ActionUrgency;
import edu.campussignal.dto.EmailActionEffectiveStatus;
import edu.campussignal.dto.EmailActionStatus;

@Service
public class ActionDeadlinePresentationService {
    private static final long DUE_SOON_MAX_DAYS = 3;
    private final Clock clock;

    public ActionDeadlinePresentationService(Clock clock) {
        this.clock = clock;
    }

    public ActionDeadlinePresentation present(EmailActionStatus status, LocalDate deadline) {
        return switch (status) {
            case COMPLETED -> terminal(
                    EmailActionEffectiveStatus.COMPLETED, ActionUrgency.COMPLETED);
            case NOT_INTERESTED -> terminal(
                    EmailActionEffectiveStatus.NOT_INTERESTED, ActionUrgency.NOT_INTERESTED);
            case PENDING -> pending(deadline);
        };
    }

    private ActionDeadlinePresentation pending(LocalDate deadline) {
        if (deadline == null) {
            return new ActionDeadlinePresentation(
                    EmailActionEffectiveStatus.PENDING,
                    ActionUrgency.NO_DEADLINE,
                    null);
        }
        long daysRemaining = ChronoUnit.DAYS.between(
                LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC), deadline);
        if (daysRemaining < 0) {
            return new ActionDeadlinePresentation(
                    EmailActionEffectiveStatus.MISSED,
                    ActionUrgency.MISSED,
                    null);
        }
        ActionUrgency urgency = urgencyForPendingDeadline(daysRemaining);
        return new ActionDeadlinePresentation(
                EmailActionEffectiveStatus.PENDING,
                urgency,
                Math.toIntExact(daysRemaining));
    }

    private ActionUrgency urgencyForPendingDeadline(long daysRemaining) {
        if (daysRemaining == 0) {
            return ActionUrgency.DUE_TODAY;
        }
        return daysRemaining <= DUE_SOON_MAX_DAYS
                ? ActionUrgency.DUE_SOON
                : ActionUrgency.UPCOMING;
    }

    private ActionDeadlinePresentation terminal(
            EmailActionEffectiveStatus effectiveStatus,
            ActionUrgency urgency) {
        return new ActionDeadlinePresentation(effectiveStatus, urgency, null);
    }
}

record ActionDeadlinePresentation(
        EmailActionEffectiveStatus effectiveStatus,
        ActionUrgency urgency,
        Integer daysRemaining) {}

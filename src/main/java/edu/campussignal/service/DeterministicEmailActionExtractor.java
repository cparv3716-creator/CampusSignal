package edu.campussignal.service;

import java.net.URI;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import edu.campussignal.dto.ActionCandidate;
import edu.campussignal.dto.EmailActionType;

@Service
public class DeterministicEmailActionExtractor {
    private static final Pattern COMPLETED_ANNOUNCEMENT = Pattern.compile(
            "\\b(?:application|registration|form)\\s+(?:was\\s+)?"
                    + "(?:submitted|completed|received)\\b"
                    + "|\\bsuccessfully\\s+(?:submitted|registered|applied)\\b"
                    + "|\\bno further action is required\\b");
    private static final Pattern SUBMIT = Pattern.compile(
            "\\b(?:submit|submission|upload)\\b");
    private static final Pattern REGISTER = Pattern.compile(
            "\\b(?:register|registration|sign\\s+up)\\b");
    private static final Pattern APPLY = Pattern.compile(
            "\\b(?:apply|application|applications\\s+open|applications\\s+close)\\b");
    private static final Pattern ATTEND = Pattern.compile(
            "\\b(?:please|must|required\\s+to)\\s+(?:attend|join|participate)\\b"
                    + "|\\b(?:attend|join)\\s+(?:the|this|our|us)\\b");
    private static final Pattern OTHER_ACTION = Pattern.compile(
            "\\b(?:action required|please complete)\\b");
    private static final Pattern ACTION_URL_CONTEXT = Pattern.compile(
            "\\b(?:register|registration|sign\\s+up|apply|application|submit|submission|"
                    + "upload|attend|join|participate)\\b");
    private static final Pattern DEADLINE_CONTEXT = Pattern.compile(
            "(?:deadline(?:\\s+is)?|last date(?:\\s+is)?|close(?:s)?\\s+on|"
                    + "applications?.{0,80}close(?:s)?(?:\\s+on)?|"
                    + "register(?:ation)?.{0,40}\\sby|apply.{0,40}\\sby|"
                    + "submit.{0,40}\\sby)\\s*$",
            Pattern.DOTALL);
    private static final Pattern URL = Pattern.compile(
            "https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DAY_FULL_MONTH = formatter("d MMMM uuuu");
    private static final DateTimeFormatter DAY_SHORT_MONTH = formatter("d MMM uuuu");
    private static final DateTimeFormatter MONTH_DAY = formatter("MMMM d uuuu");
    private static final String MONTHS = "January|February|March|April|May|June|July|August|"
            + "September|October|November|December";
    private static final String SHORT_MONTHS = "Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec";
    private static final List<DateRule> DATE_RULES = List.of(
            new DateRule(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b"),
                    LocalDate::parse),
            new DateRule(Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b"),
                    DeterministicEmailActionExtractor::parseUnambiguousNumeric),
            new DateRule(Pattern.compile(
                    "\\b\\d{1,2}\\s+(?:" + MONTHS + ")\\s+\\d{4}\\b",
                    Pattern.CASE_INSENSITIVE), value -> LocalDate.parse(value, DAY_FULL_MONTH)),
            new DateRule(Pattern.compile(
                    "\\b\\d{1,2}\\s+(?:" + SHORT_MONTHS + ")\\s+\\d{4}\\b",
                    Pattern.CASE_INSENSITIVE), value -> LocalDate.parse(value, DAY_SHORT_MONTH)),
            new DateRule(Pattern.compile(
                    "\\b(?:" + MONTHS + ")\\s+\\d{1,2},?\\s+\\d{4}\\b",
                    Pattern.CASE_INSENSITIVE), value -> LocalDate.parse(
                            value.replace(",", ""), MONTH_DAY)));

    public Optional<ActionCandidate> extract(String subject, String body) {
        String text = String.join("\n",
                Objects.requireNonNullElse(subject, ""),
                Objects.requireNonNullElse(body, ""));
        String normalized = text.toLowerCase(Locale.ROOT);
        if (COMPLETED_ANNOUNCEMENT.matcher(normalized).find()) {
            return Optional.empty();
        }
        EmailActionType actionType = actionType(normalized);
        if (actionType == null) {
            return Optional.empty();
        }
        return Optional.of(new ActionCandidate(
                actionType,
                deadline(text, normalized),
                actionUrl(text)));
    }

    private EmailActionType actionType(String text) {
        if (SUBMIT.matcher(text).find()) {
            return EmailActionType.SUBMIT;
        }
        if (REGISTER.matcher(text).find()) {
            return EmailActionType.REGISTER;
        }
        if (APPLY.matcher(text).find()) {
            return EmailActionType.APPLY;
        }
        if (ATTEND.matcher(text).find()) {
            return EmailActionType.ATTEND;
        }
        return OTHER_ACTION.matcher(text).find() ? EmailActionType.OTHER : null;
    }

    private LocalDate deadline(String original, String normalized) {
        for (DateRule rule : DATE_RULES) {
            for (MatchResult match : rule.pattern().matcher(original).results().toList()) {
                if (hasDeadlineContext(normalized, match.start())) {
                    try {
                        return rule.parser().apply(match.group());
                    } catch (DateTimeException | NumberFormatException ignored) {
                        // A recognized but invalid/ambiguous date is deliberately not invented.
                    }
                }
            }
        }
        return null;
    }

    private boolean hasDeadlineContext(String text, int dateStart) {
        int contextStart = Math.max(0, dateStart - 120);
        return DEADLINE_CONTEXT.matcher(text.substring(contextStart, dateStart)).find();
    }

    private String actionUrl(String text) {
        var matcher = URL.matcher(text);
        while (matcher.find()) {
            String candidate = stripTrailingPunctuation(matcher.group());
            if (hasActionUrlContext(text, matcher.start()) && safeWebUri(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean hasActionUrlContext(String text, int urlStart) {
        int sentenceStart = Math.max(
                Math.max(text.lastIndexOf('.', urlStart - 1), text.lastIndexOf('!', urlStart - 1)),
                Math.max(text.lastIndexOf('?', urlStart - 1), text.lastIndexOf('\n', urlStart - 1)));
        return ACTION_URL_CONTEXT.matcher(text.substring(sentenceStart + 1, urlStart)
                .toLowerCase(Locale.ROOT)).find();
    }

    private boolean safeWebUri(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String stripTrailingPunctuation(String value) {
        return value.replaceFirst("[),.;:!?]+$", "");
    }

    private static LocalDate parseUnambiguousNumeric(String value) {
        String[] parts = value.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        if (day <= 12 || month > 12) {
            throw new DateTimeException("Ambiguous or invalid numeric date");
        }
        return LocalDate.of(Integer.parseInt(parts[2]), month, day);
    }

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH);
    }

    private record DateRule(Pattern pattern, Function<String, LocalDate> parser) {}
}

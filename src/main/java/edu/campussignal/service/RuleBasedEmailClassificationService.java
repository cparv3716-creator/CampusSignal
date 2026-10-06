package edu.campussignal.service;

import edu.campussignal.dto.ClassificationConfidence;
import edu.campussignal.dto.EmailCategory;
import edu.campussignal.dto.EmailClassificationResult;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic first-version classifier defined by CLASSIFICATION_SPEC_V1.md. */
public final class RuleBasedEmailClassificationService implements EmailClassificationService {
    private static final int STRONG_SUBJECT_WEIGHT = 3;
    private static final int STRONG_BODY_WEIGHT = 2;
    private static final int CONTEXT_WEIGHT = 1;

    private static final Pattern SUBJECT_PREFIX = Pattern.compile("^(?:(?:re|fwd?)\\s*:\\s*)+");
    private static final Pattern QUOTE_START = Pattern.compile(
            "(?im)^\\s*(?:on\\s+.+\\s+wrote\\s*:|[-–— ]*forwarded message[-–— ]*|>)");
    private static final Pattern IGNORED_TRAILING_ZONE = Pattern.compile(
            "(?im)^\\s*(?:--\\s*$|unsubscribe\\b|mailing[- ]list footer\\b|"
                    + "you received this message because\\b)");
    private static final Pattern URL = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern EMAIL_ADDRESS = Pattern.compile(
            "(?i)\\b[\\p{L}\\p{N}._%+\\-]+@[\\p{L}\\p{N}.\\-]+\\.[\\p{L}]{2,}\\b");
    private static final Pattern DASH_OR_PUNCTUATION = Pattern.compile("[\\p{Pd}\\p{Punct}\\u2212]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Override
    public EmailClassificationResult classify(String subject, String body) {
        BodySections sections = splitBody(body);
        String normalizedSubject = normalizeSubject(subject);
        String currentBody = normalizeEvidence(sections.current());

        EmailClassificationResult current = classifyCurrent(normalizedSubject, currentBody);
        if (current.category() != EmailCategory.GENERAL_OTHER
                || current.confidence() != ClassificationConfidence.LOW) {
            return current;
        }

        String quotedBody = normalizeEvidence(sections.quoted());
        if (quotedBody.isBlank()) {
            return current;
        }

        EmailClassificationResult rescued = classifyCurrent("", quotedBody);
        if (rescued.category() == EmailCategory.GENERAL_OTHER) {
            return current;
        }

        // Quote-only evidence is a rescue and can never produce high confidence.
        return new EmailClassificationResult(rescued.category(), ClassificationConfidence.MEDIUM);
    }

    private EmailClassificationResult classifyCurrent(String subject, String body) {
        boolean resolvedNegation = hasNegatedCategoryCue(subject) || hasNegatedCategoryCue(body);
        Evidence career = careerEvidence(subject, body);
        if (career.qualifies()) {
            return result(EmailCategory.CAREER, career, resolvedNegation);
        }

        Evidence administrative = administrativeEvidence(subject, body);
        if (administrative.qualifies()) {
            return result(EmailCategory.ADMINISTRATIVE_NOTICE, administrative, resolvedNegation);
        }

        // Completed-state evidence is a hard gate. This is checked before the
        // setting (academic/event) because the completed achievement is the purpose.
        Evidence recognition = recognitionEvidence(subject, body);
        if (recognition.qualifies()) {
            return result(EmailCategory.RECOGNITION_ACHIEVEMENT, recognition, resolvedNegation);
        }

        Evidence academic = academicEvidence(subject, body);
        Evidence event = eventEvidence(subject, body);
        if (isUnresolvedEqualPurpose(academic, event)) {
            return new EmailClassificationResult(EmailCategory.GENERAL_OTHER, ClassificationConfidence.LOW);
        }
        if (academic.qualifies()) {
            return result(EmailCategory.ACADEMIC_RESEARCH, academic, resolvedNegation);
        }

        Evidence wellbeing = wellbeingEvidence(subject, body);
        if (wellbeing.qualifies()) {
            return result(EmailCategory.WELLBEING_SUPPORT, wellbeing, resolvedNegation);
        }

        Evidence participation = participationEvidence(subject, body);
        if (participation.qualifies()) {
            return result(EmailCategory.CALL_FOR_PARTICIPATION, participation, resolvedNegation);
        }

        if (event.qualifies()) {
            return result(EmailCategory.EVENT_ACTIVITY, event, resolvedNegation);
        }

        Evidence knownGeneralPurpose = generalPurposeEvidence(subject, body);
        if (knownGeneralPurpose.qualifies()) {
            return result(EmailCategory.GENERAL_OTHER, knownGeneralPurpose, resolvedNegation);
        }

        return new EmailClassificationResult(EmailCategory.GENERAL_OTHER, ClassificationConfidence.LOW);
    }

    private boolean isUnresolvedEqualPurpose(Evidence first, Evidence second) {
        return first.independentlyStrong()
                && second.independentlyStrong()
                && first.score() == second.score();
    }

    private Evidence careerEvidence(String subject, String body) {
        boolean subjectStrong = containsAnyUnnegated(subject,
                "internship", "placement", "pre placement", "job", "hiring", "graduate role")
                || containsAll(subject, "graduate", "role");
        boolean bodyStrong = containsAnyUnnegated(body,
                "internship", "placement", "job", "hiring", "employer")
                || (containsAll(body, "graduate", "role")
                && containsAny(body, "application", "resume", "salary", "stipend", "eligibility"));
        boolean subjectContext = containsAny(subject, "career", "graduate", "role");
        boolean bodyContext = containsAny(body,
                "salary", "stipend", "resume", "compensation", "eligibility", "application", "applicant");
        return new Evidence(subjectStrong, bodyStrong, subjectContext, bodyContext);
    }

    private Evidence administrativeEvidence(String subject, String body) {
        boolean subjectStrong = containsAny(subject,
                "circular", "protocol", "closure", "closed", "unavailable", "outage",
                "will resume", "reopens", "reopening", "service status", "access protocol");
        boolean bodyStrong = containsAny(body,
                "mandatory", "must follow", "closed", "closure", "unavailable", "outage",
                "will resume", "reopens", "reopened", "service suspended");
        if (!subjectStrong && !bodyStrong && isFacilityFeedback(subject, body)) {
            // Operational feedback is specific enough to qualify as administrative,
            // but without policy/status evidence it remains medium confidence.
            return new Evidence(false, true, false, true);
        }

        boolean subjectContext = containsAny(subject,
                "status", "facility", "service", "pool", "laboratory", "library", "hostel");
        boolean bodyContext = containsAny(body,
                "instructions", "access", "effective", "operating hours", "service", "facility");
        return new Evidence(subjectStrong, bodyStrong, subjectContext, bodyContext);
    }

    private boolean isFacilityFeedback(String subject, String body) {
        String currentMessage = subject + " " + body;
        boolean asksForFeedback = containsAny(currentMessage,
                "feedback", "survey", "comment", "questionnaire");
        boolean identifiesInstituteService = containsAny(currentMessage,
                "facility", "service", "dining", "hostel internet", "library");
        boolean concernsOperation = containsAny(currentMessage,
                "operation", "operating hours", "timings", "quality", "service", "facility");
        return asksForFeedback && identifiesInstituteService && concernsOperation;
    }

    private Evidence recognitionEvidence(String subject, String body) {
        boolean subjectCompleted = containsCompletedState(subject);
        boolean bodyCompleted = containsCompletedState(body);
        boolean subjectAchievement = containsAny(subject,
                "award", "achievement", "first place", "winner", "honour", "honor");
        boolean bodyAchievement = containsAny(body,
                "award", "achievement", "first place", "winner", "honour", "honor", "competition");

        return new Evidence(
                subjectCompleted && subjectAchievement,
                bodyCompleted && bodyAchievement,
                subjectAchievement,
                bodyAchievement);
    }

    private Evidence academicEvidence(String subject, String body) {
        boolean subjectStrong = containsAny(subject,
                "seminar", "course", "quiz", "assessment", "academic integrity",
                "scholarly lecture", "research workshop");
        boolean bodyStrong = containsAny(body,
                "seminar", "course", "quiz", "assessment", "academic integrity",
                "research workshop", "scholarly lecture");
        boolean subjectContext = containsAny(subject, "academic", "lecture", "research");
        boolean bodyContext = containsAny(body,
                "speaker", "abstract", "scholarly", "lecture", "coursework", "students");
        return new Evidence(subjectStrong, bodyStrong, subjectContext, bodyContext);
    }

    private Evidence wellbeingEvidence(String subject, String body) {
        boolean subjectStrong = containsAnyUnnegated(subject,
                "counselling", "counseling", "mental health", "wellbeing support",
                "wellness centre", "wellness center", "stress support");
        boolean bodyStrong = containsAnyUnnegated(body,
                "counselling", "counseling", "counsellor", "counselor", "mental health",
                "wellness centre", "wellness center", "stress support");
        boolean subjectContext = containsAny(subject, "appointment", "support service", "wellbeing", "wellness");
        boolean bodyContext = containsAny(body,
                "appointment", "walk in", "access hours", "available", "support service");
        return new Evidence(subjectStrong, bodyStrong, subjectContext, bodyContext);
    }

    private Evidence participationEvidence(String subject, String body) {
        boolean subjectStrong = containsAny(subject,
                "call for", "audition", "auditions", "applications open", "selection", "selections")
                || (containsAny(subject, "hackathon", "contest", "competition")
                && containsAny(subject, "registration", "registrations", "register"));
        boolean bodyStrong = containsAny(body,
                "audition", "auditions", "selection criteria", "applications open", "applications are open")
                || (containsAny(body, "apply", "application")
                && containsAny(body, "deadline", "eligibility", "eligible", "selection", "form"))
                || (containsAny(body, "register", "registration", "registrations", "submit", "submission")
                && containsAny(body, "team", "contest", "competition", "hackathon", "deadline"));
        boolean subjectContext = containsAny(subject,
                "application", "registration", "deadline", "coordinator", "programme", "program");
        boolean bodyContext = containsAny(body,
                "application", "apply", "form", "deadline", "eligibility", "eligible", "submit", "submission");
        return new Evidence(subjectStrong, bodyStrong, subjectContext, bodyContext);
    }

    private Evidence eventEvidence(String subject, String body) {
        boolean subjectStrong = containsAnyUnnegated(subject,
                "screening", "exhibition", "tutorial", "invitation")
                || (containsAny(subject, "film", "dance", "activity")
                && containsAny(subject, "friday", "monday", "pm", "am", "hall"));
        boolean bodyStrong = containsAnyUnnegated(body, "screening", "exhibition", "tutorial")
                || (containsAny(body, "attend", "invited", "join")
                && containsAny(body, "venue", "hall", "friday", "monday", "pm", "am"));
        boolean subjectContext = containsAny(subject, "event", "attend", "venue", "hall", "friday", "pm", "am");
        boolean bodyContext = containsAny(body, "attend", "invited", "join", "venue", "hall", "friday", "pm", "am");
        return new Evidence(subjectStrong, bodyStrong, subjectContext, bodyContext);
    }

    private Evidence generalPurposeEvidence(String subject, String body) {
        boolean subjectStrong = containsAny(subject, "auction", "merchandise order", "merchandise sale");
        boolean bodyStrong = containsAny(body, "bidding", "bidder", "merchandise order");
        boolean subjectContext = containsAny(subject, "bicycle", "cycle", "t shirt", "shirt");
        boolean bodyContext = containsAny(body, "collect", "price", "size", "order form");
        return new Evidence(subjectStrong, bodyStrong, subjectContext, bodyContext);
    }

    private EmailClassificationResult result(
            EmailCategory category,
            Evidence evidence,
            boolean resolvedNegation) {
        ClassificationConfidence confidence;
        if (evidence.corroborated()) {
            confidence = ClassificationConfidence.HIGH;
        } else if (evidence.score() > 0) {
            confidence = ClassificationConfidence.MEDIUM;
        } else {
            confidence = ClassificationConfidence.LOW;
        }
        if (resolvedNegation && confidence == ClassificationConfidence.HIGH) {
            confidence = ClassificationConfidence.MEDIUM;
        }
        return new EmailClassificationResult(category, confidence);
    }

    private boolean containsCompletedState(String text) {
        return containsAny(text,
                "wins", "won", "awarded", "received", "congratulating", "congratulations",
                "secured first place", "honoured", "honored", "selected as winner");
    }

    private boolean containsAny(String text, String... phrases) {
        String paddedText = " " + text + " ";
        for (String phrase : phrases) {
            String normalizedPhrase = normalizeEvidence(phrase);
            if (!normalizedPhrase.isBlank() && paddedText.contains(" " + normalizedPhrase + " ")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyUnnegated(String text, String... phrases) {
        for (String phrase : phrases) {
            String normalizedPhrase = normalizeEvidence(phrase);
            if (hasUnnegatedOccurrence(text, normalizedPhrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNegatedCategoryCue(String text) {
        for (String phrase : new String[] {
                "internship", "job", "placement", "employment", "event",
                "counselling", "counseling", "mental health", "support service",
                "wellbeing support", "wellness centre", "wellness center"}) {
            if (hasNegatedOccurrence(text, normalizeEvidence(phrase))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUnnegatedOccurrence(String text, String phrase) {
        String paddedText = " " + text + " ";
        String target = " " + phrase + " ";
        int occurrence = paddedText.indexOf(target);
        while (occurrence >= 0) {
            if (!isNegatedByNearbyWord(paddedText.substring(0, occurrence))) {
                return true;
            }
            occurrence = paddedText.indexOf(target, occurrence + target.length());
        }
        return false;
    }

    private boolean hasNegatedOccurrence(String text, String phrase) {
        String paddedText = " " + text + " ";
        String target = " " + phrase + " ";
        int occurrence = paddedText.indexOf(target);
        while (occurrence >= 0) {
            if (isNegatedByNearbyWord(paddedText.substring(0, occurrence))) {
                return true;
            }
            occurrence = paddedText.indexOf(target, occurrence + target.length());
        }
        return false;
    }

    private boolean isNegatedByNearbyWord(String precedingText) {
        String normalizedPrefix = precedingText.strip();
        if (normalizedPrefix.isEmpty()) {
            return false;
        }

        String[] words = normalizedPrefix.split(" ");
        int firstNearbyWord = Math.max(0, words.length - 4);
        for (int index = words.length - 1; index >= firstNearbyWord; index--) {
            if (words[index].equals("not") || words[index].equals("no") || words[index].equals("non")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAll(String text, String... words) {
        for (String word : words) {
            if (!containsAny(text, word)) {
                return false;
            }
        }
        return true;
    }

    private BodySections splitBody(String body) {
        String safeBody = body == null ? "" : body;
        Matcher quoteMatcher = QUOTE_START.matcher(safeBody);
        if (!quoteMatcher.find()) {
            return new BodySections(stripIgnoredTrailingZone(safeBody), "");
        }

        String current = stripIgnoredTrailingZone(safeBody.substring(0, quoteMatcher.start()));
        String quoted = safeBody.substring(quoteMatcher.end());
        return new BodySections(current, quoted);
    }

    private String stripIgnoredTrailingZone(String text) {
        Matcher matcher = IGNORED_TRAILING_ZONE.matcher(text);
        return matcher.find() ? text.substring(0, matcher.start()) : text;
    }

    private String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(subject, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        String withoutReplyPrefix = SUBJECT_PREFIX.matcher(normalized).replaceFirst("");
        return normalizeEvidence(withoutReplyPrefix);
    }

    private String normalizeEvidence(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        normalized = URL.matcher(normalized).replaceAll(" ");
        normalized = EMAIL_ADDRESS.matcher(normalized).replaceAll(" ");
        normalized = DASH_OR_PUNCTUATION.matcher(normalized).replaceAll(" ");
        return WHITESPACE.matcher(normalized).replaceAll(" ").strip();
    }

    private record BodySections(String current, String quoted) {
    }

    private record Evidence(
            boolean subjectStrong,
            boolean bodyStrong,
            boolean subjectContext,
            boolean bodyContext) {

        boolean qualifies() {
            return subjectStrong || bodyStrong;
        }

        int score() {
            int score = subjectStrong ? STRONG_SUBJECT_WEIGHT : 0;
            score += bodyStrong ? STRONG_BODY_WEIGHT : 0;
            score += subjectContext || bodyContext ? CONTEXT_WEIGHT : 0;
            return score;
        }

        boolean corroborated() {
            return (subjectStrong && (bodyStrong || bodyContext))
                    || (bodyStrong && subjectContext);
        }

        boolean independentlyStrong() {
            return subjectStrong && bodyStrong;
        }
    }
}

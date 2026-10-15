package edu.campussignal.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stateless normalization, filtering, quote segmentation, and negation matching. */
final class ClassificationTextPreprocessor {
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

    private ClassificationTextPreprocessor() {
    }

    static boolean containsAny(String text, String... phrases) {
        String paddedText = " " + text + " ";
        for (String phrase : phrases) {
            String normalizedPhrase = normalizeEvidence(phrase);
            if (!normalizedPhrase.isBlank() && paddedText.contains(" " + normalizedPhrase + " ")) {
                return true;
            }
        }
        return false;
    }

    static boolean containsAnyUnnegated(String text, String... phrases) {
        for (String phrase : phrases) {
            String normalizedPhrase = normalizeEvidence(phrase);
            if (hasOccurrence(text, normalizedPhrase, false)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasNegatedCategoryCue(String text) {
        for (String phrase : new String[] {
                "internship", "job", "placement", "employment", "event",
                "counselling", "counseling", "mental health", "support service",
                "wellbeing support", "wellness centre", "wellness center"}) {
            if (hasOccurrence(text, normalizeEvidence(phrase), true)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOccurrence(String text, String phrase, boolean negated) {
        String paddedText = " " + text + " ";
        String target = " " + phrase + " ";
        int occurrence = paddedText.indexOf(target);
        while (occurrence >= 0) {
            if (isNegatedByNearbyWord(paddedText.substring(0, occurrence)) == negated) {
                return true;
            }
            occurrence = paddedText.indexOf(target, occurrence + target.length());
        }
        return false;
    }

    private static boolean isNegatedByNearbyWord(String precedingText) {
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

    static boolean containsAll(String text, String... words) {
        for (String word : words) {
            if (!containsAny(text, word)) {
                return false;
            }
        }
        return true;
    }

    static BodySections splitBody(String body) {
        String safeBody = body == null ? "" : body;
        Matcher quoteMatcher = QUOTE_START.matcher(safeBody);
        if (!quoteMatcher.find()) {
            return new BodySections(stripIgnoredTrailingZone(safeBody), "");
        }

        String current = stripIgnoredTrailingZone(safeBody.substring(0, quoteMatcher.start()));
        String quoted = safeBody.substring(quoteMatcher.end());
        return new BodySections(current, quoted);
    }

    private static String stripIgnoredTrailingZone(String text) {
        Matcher matcher = IGNORED_TRAILING_ZONE.matcher(text);
        return matcher.find() ? text.substring(0, matcher.start()) : text;
    }

    static String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(subject, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        String withoutReplyPrefix = SUBJECT_PREFIX.matcher(normalized).replaceFirst("");
        return normalizeEvidence(withoutReplyPrefix);
    }

    static String normalizeEvidence(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        normalized = URL.matcher(normalized).replaceAll(" ");
        normalized = EMAIL_ADDRESS.matcher(normalized).replaceAll(" ");
        normalized = DASH_OR_PUNCTUATION.matcher(normalized).replaceAll(" ");
        return WHITESPACE.matcher(normalized).replaceAll(" ").strip();
    }

    record BodySections(String current, String quoted) {
    }
}

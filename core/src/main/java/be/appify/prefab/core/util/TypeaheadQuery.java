package be.appify.prefab.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Splits the search text of a type-ahead search into the words that are matched as prefix and, when the text uses
 * web search operators, the part that is matched as web search syntax. The prefixes only ever consist of letters and
 * digits, so they can be used to build a text search query without any risk of breaking or altering that query.
 *
 * <p>A <em>plain</em> search text has no double quotes, no excluded word (<code>-word</code>) and no
 * <code>OR</code>. Every word of a plain text is a prefix, so "spr bo g" finds "Spring Boot guide", and
 * {@link #wholeWords()} is empty. For other texts only the last word is a prefix, and only when the user is evidently
 * still typing it: the text does not end in whitespace or punctuation, the word is not negated, not inside an open
 * double quote and not <code>OR</code>. The rest is then matched as web search syntax.</p>
 *
 * @param wholeWords
 *         the search text to match as web search syntax, without the prefixes; empty for a plain text with prefixes
 * @param prefixes
 *         the words to match as prefix, possibly none
 */
public record TypeaheadQuery(String wholeWords, List<String> prefixes) {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Pattern TRAILING_WORD = Pattern.compile("[\\p{L}\\p{N}]+$");
    private static final int MIN_INITIALS_LENGTH = 2;

    /** Makes the prefixes immutable. */
    public TypeaheadQuery {
        prefixes = List.copyOf(prefixes);
    }

    /**
     * Splits a search text.
     *
     * @param query
     *         the user supplied search text, may be {@code null}
     * @return the split search text; without prefixes when the text has no words to match as prefix
     */
    public static TypeaheadQuery of(String query) {
        var text = query == null ? "" : query;
        var words = wordsOf(text);
        if (words.isEmpty()) {
            return new TypeaheadQuery(text, List.of());
        }
        return isPlain(text) ? new TypeaheadQuery("", words) : ofWebSearch(text);
    }

    /**
     * The first letters of the words of the search text, to match against the first letters of the words of a value,
     * so that "sbg" finds "Spring Boot guide". Only a plain text that is a single word of at least two characters
     * has initials.
     *
     * @return the lower case initials, if any
     */
    public Optional<String> initials() {
        return wholeWords.isEmpty() && prefixes.size() == 1 && prefixes.getFirst().length() >= MIN_INITIALS_LENGTH
                ? Optional.of(prefixes.getFirst().toLowerCase())
                : Optional.empty();
    }

    /**
     * A regular expression that matches a value when every word of the search text is the start of a word of the
     * value, in any order, or when the search text is the initials of the words of the value. Matching must be case
     * insensitive. The expression only contains letters, digits and regex syntax, so it is safe to use for any input.
     *
     * @param query
     *         the user supplied search text, may be {@code null}
     * @return the regular expression; empty, which matches everything, when the search text has no words
     */
    public static String valueRegex(String query) {
        var words = wordsOf(query == null ? "" : query);
        if (words.isEmpty()) {
            return "";
        }
        var allWords = words.stream().map(word -> "(?=.*\\b" + word + ")").collect(Collectors.joining());
        return new TypeaheadQuery("", words).initials()
                .map(initials -> allWords + "|" + initialsRegex(initials))
                .orElse(allWords);
    }

    /**
     * A regular expression that matches a value whose words start with the given initials, in order.
     *
     * @param initials
     *         the letters and digits, one per word
     * @return the regular expression, to be matched case insensitively
     */
    public static String initialsRegex(String initials) {
        return "^\\W*" + initials.chars().mapToObj(character -> String.valueOf((char) character))
                .collect(Collectors.joining("\\w*\\W+")) + "\\w*";
    }

    private static List<String> wordsOf(String text) {
        var words = new ArrayList<String>();
        var matcher = WORD.matcher(text);
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return words;
    }

    private static boolean isPlain(String text) {
        var matcher = WORD.matcher(text);
        while (matcher.find()) {
            var negated = matcher.start() > 0 && text.charAt(matcher.start() - 1) == '-';
            if (negated || "OR".equalsIgnoreCase(matcher.group())) {
                return false;
            }
        }
        return !text.contains("\"");
    }

    private static TypeaheadQuery ofWebSearch(String text) {
        var matcher = TRAILING_WORD.matcher(text);
        if (!matcher.find()) {
            return new TypeaheadQuery(text, List.of());
        }
        var head = text.substring(0, matcher.start());
        var word = matcher.group();
        var typingPlainWord = !head.endsWith("-") && quotesAreBalanced(head) && !"OR".equalsIgnoreCase(word);
        return typingPlainWord
                ? new TypeaheadQuery(head.trim(), List.of(word))
                : new TypeaheadQuery(text, List.of());
    }

    private static boolean quotesAreBalanced(String text) {
        return text.chars().filter(character -> character == '"').count() % 2 == 0;
    }
}

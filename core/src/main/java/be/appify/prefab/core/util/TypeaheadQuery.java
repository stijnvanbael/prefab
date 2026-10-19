package be.appify.prefab.core.util;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Splits the search text of a type-ahead search into the part that is matched as whole words and the word the user is
 * still typing, which is matched as a prefix. The prefix only ever consists of letters and digits, so it can be used
 * to build a text search query without any risk of breaking or altering that query.
 *
 * <p>No prefix is extracted when the user has evidently finished the last word or is not typing a plain word: the
 * text ends in whitespace or punctuation, the last word is negated (<code>-word</code>), it is inside an open double
 * quote or it is the <code>OR</code> operator.</p>
 *
 * @param wholeWords
 *         the search text to match as web search syntax, without the prefix
 * @param prefix
 *         the word to match as prefix, if any
 */
public record TypeaheadQuery(String wholeWords, Optional<String> prefix) {
    private static final Pattern TRAILING_WORD = Pattern.compile("[\\p{L}\\p{N}]+$");

    /**
     * Splits a search text.
     *
     * @param query
     *         the user supplied search text, may be {@code null}
     * @return the split search text; the prefix is empty when the last word must be matched as a whole word
     */
    public static TypeaheadQuery of(String query) {
        var text = query == null ? "" : query;
        var matcher = TRAILING_WORD.matcher(text);
        if (!matcher.find()) {
            return new TypeaheadQuery(text, Optional.empty());
        }
        var head = text.substring(0, matcher.start());
        var word = matcher.group();
        var typingPlainWord = !head.endsWith("-") && quotesAreBalanced(head) && !"OR".equalsIgnoreCase(word);
        return typingPlainWord
                ? new TypeaheadQuery(head.trim(), Optional.of(word))
                : new TypeaheadQuery(text, Optional.empty());
    }

    private static boolean quotesAreBalanced(String text) {
        return text.chars().filter(character -> character == '"').count() % 2 == 0;
    }
}

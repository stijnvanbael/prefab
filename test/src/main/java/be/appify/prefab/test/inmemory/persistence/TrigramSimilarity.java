package be.appify.prefab.test.inmemory.persistence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An approximation of the {@code pg_trgm} similarity functions, used to imitate fuzzy matching in memory. Words are
 * padded with two leading spaces and one trailing space and split in trigrams, as PostgreSQL does.
 */
final class TrigramSimilarity {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

    private TrigramSimilarity() {
    }

    /**
     * The words of a text, in lower case.
     *
     * @param text
     *         the text
     * @return the words, possibly none
     */
    static List<String> wordsOf(String text) {
        var words = new ArrayList<String>();
        var matcher = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return words;
    }

    /**
     * Equivalent of {@code similarity(a, b)}: shared trigrams divided by all trigrams of both texts.
     *
     * @param first
     *         the first text
     * @param second
     *         the second text
     * @return the similarity between 0 and 1
     */
    static double similarity(String first, String second) {
        var left = trigramsOf(wordsOf(first));
        var right = trigramsOf(wordsOf(second));
        var union = new HashSet<>(left);
        union.addAll(right);
        if (union.isEmpty()) {
            return 0;
        }
        return (double) shared(left, right) / union.size();
    }

    /**
     * Equivalent of {@code word_similarity(word, text)}: the share of the trigrams of the word that the best matching
     * run of consecutive words of the text contains.
     *
     * @param word
     *         the searched word or words
     * @param text
     *         the text to look in
     * @return the similarity between 0 and 1
     */
    static double wordSimilarity(String word, String text) {
        var wanted = trigramsOf(wordsOf(word));
        var words = wordsOf(text);
        var best = 0.0;
        for (var start = 0; start < words.size() && !wanted.isEmpty(); start++) {
            for (var end = start + 1; end <= words.size(); end++) {
                var extent = trigramsOf(words.subList(start, end));
                best = Math.max(best, (double) shared(wanted, extent) / wanted.size());
            }
        }
        return best;
    }

    private static int shared(Set<String> left, Set<String> right) {
        var common = new HashSet<>(left);
        common.retainAll(right);
        return common.size();
    }

    private static Set<String> trigramsOf(List<String> words) {
        var trigrams = new HashSet<String>();
        for (var word : words) {
            var padded = "  " + word + " ";
            for (var index = 0; index + 3 <= padded.length(); index++) {
                trigrams.add(padded.substring(index, index + 3));
            }
        }
        return trigrams;
    }
}

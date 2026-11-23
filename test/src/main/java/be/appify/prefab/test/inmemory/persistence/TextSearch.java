package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.spring.data.FullTextCondition;
import be.appify.prefab.core.util.TypeaheadQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Evaluates a {@link FullTextCondition} against the searched property values of one aggregate. An approximation of
 * PostgreSQL text search with the {@code simple} configuration: no stemming and no stop words.
 *
 * <ul>
 *   <li>Type-ahead prefixes must all be the start of a word in any searched property, or the single prefix must be
 *       the initials of a property value.</li>
 *   <li>The remaining text follows web search syntax: words are required, {@code "quoted phrases"} must occur in a
 *       single property, {@code -word} excludes and {@code or} separates alternatives.</li>
 *   <li>A fuzzy condition also accepts values where every word is similar to a word of a searched property.</li>
 *   <li>The score of a match is the sum of the weights of the properties that matched each word, so a hit in a
 *       heavier property ranks higher. PostgreSQL ranks by {@code ts_rank_cd}, which also considers frequency and
 *       proximity, so the order of results with equal weights can differ.</li>
 * </ul>
 */
final class TextSearch {
    private static final Pattern TERM = Pattern.compile("(-?)(?:\"([^\"]*)\"|([\\p{L}\\p{N}]+))");

    private final FullTextCondition condition;
    private final TypeaheadQuery typeahead;
    private final List<List<Term>> alternatives;

    TextSearch(FullTextCondition condition) {
        this.condition = condition;
        this.typeahead = condition.typeaheadQuery();
        this.alternatives = alternativesOf(typeahead.wholeWords());
    }

    /**
     * Scores an aggregate.
     *
     * @param values
     *         the text values of the searched properties, by property name
     * @return the score, empty when the aggregate does not match
     */
    OptionalDouble score(Values values) {
        if (!condition.isActive()) {
            return OptionalDouble.of(0);
        }
        var exact = exactScore(values);
        return exact.isPresent() || !condition.fuzzy() ? exact : fuzzyScore(values);
    }

    private OptionalDouble exactScore(Values values) {
        var prefixScore = prefixScore(values);
        var webScore = alternatives.isEmpty() ? OptionalDouble.of(0) : webScore(values);
        return prefixScore.isPresent() && webScore.isPresent()
                ? OptionalDouble.of(prefixScore.getAsDouble() + webScore.getAsDouble())
                : OptionalDouble.empty();
    }

    private OptionalDouble prefixScore(Values values) {
        if (typeahead.prefixes().isEmpty()) {
            return OptionalDouble.of(0);
        }
        var total = 0.0;
        var allPrefixesMatch = true;
        for (var prefix : typeahead.prefixes()) {
            var best = values.bestWeight(words -> words.stream().anyMatch(word -> word.startsWith(prefix)));
            allPrefixesMatch &= best.isPresent();
            total += best.orElse(0);
        }
        if (allPrefixesMatch) {
            return OptionalDouble.of(total);
        }
        return typeahead.initials()
                .map(initials -> Pattern.compile(TypeaheadQuery.initialsRegex(initials),
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
                .map(pattern -> values.bestWeightOfText(text -> pattern.matcher(text).find()))
                .orElseGet(OptionalDouble::empty);
    }

    private OptionalDouble webScore(Values values) {
        OptionalDouble best = OptionalDouble.empty();
        for (var terms : alternatives) {
            var score = scoreOf(terms, values);
            if (score.isPresent() && (best.isEmpty() || score.getAsDouble() > best.getAsDouble())) {
                best = score;
            }
        }
        return best;
    }

    private static OptionalDouble scoreOf(List<Term> terms, Values values) {
        var total = 0.0;
        for (var term : terms) {
            var weight = values.bestWeight(term::occursIn);
            if (term.negated() == weight.isPresent()) {
                return OptionalDouble.empty();
            }
            total += term.negated() ? 0 : weight.getAsDouble();
        }
        return OptionalDouble.of(total);
    }

    // Every plain word must be similar to a word of a searched property, or be the prefix being typed
    private OptionalDouble fuzzyScore(Values values) {
        var words = plainWordsOf(condition.query());
        if (words.isEmpty()) {
            return OptionalDouble.empty();
        }
        var total = 0.0;
        for (var word : words) {
            var weight = values.bestWeightOfText(text ->
                    TrigramSimilarity.wordSimilarity(word, text) >= condition.similarityThreshold());
            if (weight.isEmpty() && typeahead.prefixes().contains(word.toLowerCase(Locale.ROOT))) {
                weight = values.bestWeight(known -> known.stream().anyMatch(candidate -> candidate.startsWith(word)));
            }
            if (weight.isEmpty()) {
                return OptionalDouble.empty();
            }
            total += weight.getAsDouble();
        }
        return OptionalDouble.of(total);
    }

    private static List<String> plainWordsOf(String query) {
        var words = new ArrayList<String>();
        var matcher = Pattern.compile("[\\p{L}\\p{N}]+").matcher(query);
        while (matcher.find()) {
            var negated = matcher.start() > 0 && query.charAt(matcher.start() - 1) == '-';
            if (!negated && !"OR".equalsIgnoreCase(matcher.group())) {
                words.add(matcher.group());
            }
        }
        return words;
    }

    private static List<List<Term>> alternativesOf(String webSearch) {
        var alternatives = new ArrayList<List<Term>>();
        var current = new ArrayList<Term>();
        var matcher = TERM.matcher(webSearch);
        while (matcher.find()) {
            var phrase = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if ("or".equalsIgnoreCase(phrase) && matcher.group(2) == null && matcher.group(1).isEmpty()) {
                addIfNotEmpty(alternatives, current);
                current = new ArrayList<>();
                continue;
            }
            var words = TrigramSimilarity.wordsOf(phrase);
            if (!words.isEmpty()) {
                current.add(new Term(words, !matcher.group(1).isEmpty()));
            }
        }
        addIfNotEmpty(alternatives, current);
        return alternatives;
    }

    private static void addIfNotEmpty(List<List<Term>> alternatives, List<Term> terms) {
        if (!terms.isEmpty()) {
            alternatives.add(terms);
        }
    }

    private record Term(List<String> words, boolean negated) {

        boolean occursIn(List<String> candidate) {
            return Collections.indexOfSubList(candidate, words) >= 0;
        }
    }

    /**
     * The words of the searched properties of one aggregate.
     *
     * @param texts
     *         the text of every searched property
     * @param condition
     *         the condition that defines the weights
     */
    record Values(List<PropertyText> texts, FullTextCondition condition) {

        OptionalDouble bestWeight(Predicate<List<String>> wordsMatch) {
            return texts.stream()
                    .filter(text -> wordsMatch.test(text.words()))
                    .mapToDouble(text -> condition.weightOf(text.property()).textIndexWeight())
                    .max();
        }

        OptionalDouble bestWeightOfText(Predicate<String> valueMatches) {
            return texts.stream()
                    .filter(text -> text.values().stream().anyMatch(valueMatches))
                    .mapToDouble(text -> condition.weightOf(text.property()).textIndexWeight())
                    .max();
        }
    }

    /**
     * The text of one searched property.
     *
     * @param property
     *         the property name
     * @param values
     *         the text values, one for a string and several for a collection
     */
    record PropertyText(String property, Collection<String> values) {

        List<String> words() {
            return values.stream().flatMap(value -> TrigramSimilarity.wordsOf(value).stream()).toList();
        }
    }
}

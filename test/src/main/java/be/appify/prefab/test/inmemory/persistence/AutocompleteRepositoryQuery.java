package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.core.annotations.rest.AutocompleteQuery;
import be.appify.prefab.core.annotations.rest.MatchStrategy;
import be.appify.prefab.core.annotations.rest.ScanMode;
import be.appify.prefab.core.util.TypeaheadQuery;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Executes a generated {@code autocompleteBy<Field>} method in memory, using the {@link AutocompleteQuery} metadata
 * the processor puts on it instead of the native SQL or aggregation pipeline. Returns the distinct, sorted values of
 * the property that match the query.
 *
 * <p>The generated method takes {@code (String query, int limit, long offset)} with JDBC and
 * {@code (String query, Pageable pageable)} otherwise. For the full-text strategies the generated MongoDB service
 * passes a ready made regular expression instead of the text the user typed, which is matched case insensitively.</p>
 */
final class AutocompleteRepositoryQuery implements RepositoryQuery {
    private static final double SIMILARITY_THRESHOLD = 0.3;

    private final QueryMethod queryMethod;
    private final AutocompleteQuery metadata;
    private final InMemoryStore store;
    private final Class<?> domainType;

    AutocompleteRepositoryQuery(
            QueryMethod queryMethod,
            AutocompleteQuery metadata,
            InMemoryStore store,
            Class<?> domainType
    ) {
        this.queryMethod = queryMethod;
        this.metadata = metadata;
        this.store = store;
        this.domainType = domainType;
    }

    @Override
    public Object execute(Object[] parameters) {
        var query = (String) parameters[0];
        var matches = matcher(query, hasLimitAndOffset(parameters));
        var values = store.findAll(domainType).stream()
                .flatMap(aggregate -> store.properties().read(aggregate, metadata.property()).stream())
                .map(Object::toString)
                .filter(matches)
                .distinct()
                .sorted()
                .toList();
        return slice(values, parameters);
    }

    private static List<String> slice(List<String> values, Object[] parameters) {
        var offset = hasLimitAndOffset(parameters)
                ? ((Number) parameters[2]).longValue()
                : ((Pageable) parameters[1]).getOffset();
        var limit = hasLimitAndOffset(parameters)
                ? ((Number) parameters[1]).longValue()
                : (long) ((Pageable) parameters[1]).getPageSize();
        return values.stream().skip(offset).limit(limit).toList();
    }

    private static boolean hasLimitAndOffset(Object[] parameters) {
        return parameters.length > 2;
    }

    private Predicate<String> matcher(String query, boolean rawQuery) {
        var scanMode = metadata.scanMode();
        return switch (metadata.matchStrategy()) {
            case EXACT -> value -> scan(scanMode, value, query);
            case IGNORE_CASE -> value -> scan(scanMode, lower(value), lower(query));
            case FUZZY -> value -> scanMode == ScanMode.PREFIX
                    ? TrigramSimilarity.wordSimilarity(query, value) >= SIMILARITY_THRESHOLD
                    : TrigramSimilarity.similarity(value, query) > SIMILARITY_THRESHOLD;
            case FULL_TEXT, FULL_TEXT_FUZZY -> fullText(query, rawQuery, metadata.matchStrategy());
        };
    }

    private static boolean scan(ScanMode scanMode, String value, String query) {
        return scanMode == ScanMode.PREFIX ? value.startsWith(query) : value.contains(query);
    }

    private static String lower(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    // The typed words are all matched as prefix of a word of the value, or the single word as initials of the value
    private static Predicate<String> fullText(String query, boolean rawQuery, MatchStrategy strategy) {
        if (!rawQuery) {
            var pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            return value -> pattern.matcher(value).find();
        }
        var words = TrigramSimilarity.wordsOf(query);
        var initials = TypeaheadQuery.of(query).initials()
                .map(typed -> Pattern.compile(TypeaheadQuery.initialsRegex(typed),
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        return value -> {
            var valueWords = TrigramSimilarity.wordsOf(value);
            var prefixed = words.stream().allMatch(word -> valueWords.stream().anyMatch(known -> known.startsWith(word)));
            var initialsMatch = initials.map(pattern -> pattern.matcher(value).find()).orElse(false);
            var similar = strategy == MatchStrategy.FULL_TEXT_FUZZY && words.stream().allMatch(word ->
                    valueWords.stream().anyMatch(known -> known.startsWith(word))
                            || TrigramSimilarity.wordSimilarity(word, value) >= SIMILARITY_THRESHOLD);
            return prefixed || initialsMatch || similar;
        };
    }

    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }
}

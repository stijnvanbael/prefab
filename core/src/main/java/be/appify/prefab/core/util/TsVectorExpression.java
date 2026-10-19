package be.appify.prefab.core.util;

import be.appify.prefab.core.annotations.rest.Filter.Weight;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the PostgreSQL tsvector expression of a full-text search. Shared by the generated index and the query
 * executor so that the index stays usable by the query.
 */
public final class TsVectorExpression {
    private TsVectorExpression() {
    }

    /**
     * Builds the expression. Weights are only applied when at least one column has a weight other than the default,
     * because the default weight is what an unlabelled vector has.
     *
     * @param configuration
     *         the text search configuration, for example {@code simple}
     * @param weightByColumn
     *         the weight of each searched column, in index order
     * @return the SQL expression
     */
    public static String of(String configuration, Map<String, Weight> weightByColumn) {
        var weighted = weightByColumn.values().stream().anyMatch(weight -> weight != Weight.D);
        // concat_ws is not IMMUTABLE and cannot be used in an index; the || form is
        if (!weighted) {
            var text = weightByColumn.keySet().stream().map(TsVectorExpression::textOf)
                    .collect(Collectors.joining(" || ' ' || "));
            return "to_tsvector('%s', %s)".formatted(configuration, text);
        }
        return weightByColumn.entrySet().stream()
                .map(entry -> "setweight(to_tsvector('%s', %s), '%s')"
                        .formatted(configuration, textOf(entry.getKey()), entry.getValue()))
                .collect(Collectors.joining(" || "));
    }

    private static String textOf(String column) {
        return "coalesce(\"%s\", '')".formatted(column);
    }
}


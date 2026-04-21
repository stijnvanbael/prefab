package be.appify.prefab.core.util;

import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import java.util.List;

/**
 * Converts Java property names to PostgreSQL-safe column identifiers.
 * <p>
 * The identifier is first converted to snake_case, then deduplicated by removing repeated segments,
 * and finally shortened to stay within PostgreSQL's 63-character identifier length limit.
 * </p>
 */
public final class IdentifierShortener {

    public static final int POSTGRES_MAX_IDENTIFIER_LENGTH = 63;

    private static final int MIN_PREFIX_SEGMENT_LENGTH = 2;
    private static final int MIN_LAST_SEGMENT_LENGTH = 3;
    private static final Pattern CAMEL_CASE_BOUNDARY = Pattern.compile("([a-z])([A-Z]+)");

    private IdentifierShortener() {
    }

    /**
     * Converts a camelCase property name to a PostgreSQL-safe column identifier.
     * Repeated segments in the resulting snake_case path are removed before any truncation is applied.
     *
     * @param rawName the camelCase property name
     * @return a snake_case identifier, deduplicated and shortened to the PostgreSQL limit
     */
    public static String columnName(String rawName) {
        return shorten(deduplicate(toSnakeCase(rawName)), POSTGRES_MAX_IDENTIFIER_LENGTH);
    }

    /**
     * Builds a shortened index name from a table and column name with the appropriate suffix.
     *
     * @param tableName  the table name
     * @param columnName the column name
     * @param unique     whether the index is unique
     * @return a PostgreSQL-safe index name
     */
    public static String indexName(String tableName, String columnName, boolean unique) {
        var suffix = unique ? "uk" : "ix";
        return shortenWithSuffix(tableName + "_" + columnName, suffix, POSTGRES_MAX_IDENTIFIER_LENGTH);
    }

    /**
     * Builds a shortened foreign key constraint name from a table and column name.
     *
     * @param tableName  the table name
     * @param columnName the column name
     * @return a PostgreSQL-safe foreign key constraint name
     */
    public static String foreignKeyConstraintName(String tableName, String columnName) {
        return shortenWithSuffix(tableName + "_" + columnName, "fk", POSTGRES_MAX_IDENTIFIER_LENGTH);
    }

    /**
     * Shortens an already-snake_case identifier to the given maximum length by trimming segments.
     *
     * @param identifier the snake_case identifier
     * @param maxLength  the maximum allowed length
     * @return a shortened identifier
     */
    public static String shorten(String identifier, int maxLength) {
        if (identifier.length() <= maxLength) {
            return identifier;
        }

        var segments = identifier.split("_");
        if (segments.length <= 1) {
            return identifier.substring(0, maxLength);
        }

        var segmentLengths = originalLengths(segments);
        reduceSegmentLengths(segments, segmentLengths, maxLength);

        var shortened = joinSegments(segments, segmentLengths);
        return shortened.length() <= maxLength ? shortened : shortened.substring(0, maxLength);
    }

    private static String toSnakeCase(String value) {
        return CAMEL_CASE_BOUNDARY.matcher(value).replaceAll("$1_$2").toLowerCase();
    }

    /**
     * Removes duplicate segments from a snake_case identifier, keeping the first occurrence of each segment.
     * For example, {@code person_name_first_name} becomes {@code person_name_first}.
     *
     * @param identifier the snake_case identifier
     * @return the identifier with duplicate segments removed
     */
    public static String deduplicate(String identifier) {
        return String.join("_", new LinkedHashSet<String>(List.of(identifier.split("_", -1))));
    }

    private static String shortenWithSuffix(String base, String suffix, int maxLength) {
        var fullName = base + "_" + suffix;
        if (fullName.length() <= maxLength) {
            return fullName;
        }

        var maxBaseLength = maxLength - suffix.length() - 1;
        if (maxBaseLength <= 0) {
            return fullName.substring(0, maxLength);
        }

        return shorten(base, maxBaseLength) + "_" + suffix;
    }

    private static int[] originalLengths(String[] segments) {
        var lengths = new int[segments.length];
        for (int i = 0; i < segments.length; i++) {
            lengths[i] = segments[i].length();
        }
        return lengths;
    }

    private static void reduceSegmentLengths(String[] segments, int[] segmentLengths, int maxLength) {
        while (joinedLength(segmentLengths) > maxLength) {
            if (!reduceOnce(segments, segmentLengths, maxLength)) {
                return;
            }
        }
    }

    private static boolean reduceOnce(String[] segments, int[] segmentLengths, int maxLength) {
        var reduced = false;

        for (int i = 0; i < segments.length - 1; i++) {
            var minLength = Math.min(MIN_PREFIX_SEGMENT_LENGTH, segments[i].length());
            if (segmentLengths[i] > minLength) {
                segmentLengths[i]--;
                reduced = true;
                if (joinedLength(segmentLengths) <= maxLength) {
                    return true;
                }
            }
        }

        var lastIndex = segments.length - 1;
        var minLastLength = Math.min(MIN_LAST_SEGMENT_LENGTH, segments[lastIndex].length());
        if (segmentLengths[lastIndex] > minLastLength) {
            segmentLengths[lastIndex]--;
            reduced = true;
        }

        return reduced;
    }

    private static int joinedLength(int[] segmentLengths) {
        var total = 0;
        for (int length : segmentLengths) {
            total += length;
        }
        return total + segmentLengths.length - 1;
    }

    private static String joinSegments(String[] segments, int[] lengths) {
        var builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                builder.append('_');
            }
            builder.append(segments[i], 0, lengths[i]);
        }
        return builder.toString();
    }
}


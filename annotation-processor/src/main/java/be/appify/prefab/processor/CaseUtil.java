package be.appify.prefab.processor;

/** Utility class for case conversions. */
public class CaseUtil {
    private CaseUtil() {
    }

    /**
     * Converts a camelCase string to kebab-case.
     *
     * @param value The camelCase string.
     * @return The kebab-case string.
     */
    public static String toKebabCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
    }

    /**
     * Converts a camelCase string to snake_case.
     *
     * @param value The camelCase string.
     * @return The snake_case string.
     */
    public static String toSnakeCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    /**
     * Converts a string with underscores or hyphens to camelCase.
     *
     * @param value The string with underscores or hyphens.
     * @return The camelCase string.
     */
    public static String toCamelCase(String value) {
        return value.replaceAll("(\\w)[_\\-](\\w)", "$1$2"); // TODO: correct impl
    }
}

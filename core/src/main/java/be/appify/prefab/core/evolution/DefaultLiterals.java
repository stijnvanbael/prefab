package be.appify.prefab.core.evolution;

import java.util.List;

/**
 * Parses the literal of a {@link be.appify.prefab.core.annotations.Default} annotation into a value of the component
 * type.
 */
public final class DefaultLiterals {
    private static final String EMPTY_LIST = "[]";

    private DefaultLiterals() {
    }

    /**
     * Returns whether a default can be declared for a component of the given type.
     *
     * @param type the component type
     * @return {@code true} if a default literal can be parsed for the type
     */
    public static boolean supports(Class<?> type) {
        return type == String.class
                || type.isEnum()
                || type == List.class
                || isNumeric(type)
                || type == boolean.class || type == Boolean.class;
    }

    /**
     * Parses the literal into a value of the component type.
     *
     * @param type    the component type
     * @param literal the literal declared on the component
     * @return the default value: a {@code String}, {@code Boolean}, {@code Integer}, {@code Long}, {@code Float} or
     * {@code Double}, the constant name for enums, or an empty list for lists
     * @throws IllegalArgumentException if the literal is not valid for the type
     */
    public static Object parse(Class<?> type, String literal) {
        try {
            if (type == String.class) {
                return literal;
            } else if (type == int.class || type == Integer.class) {
                return Integer.parseInt(literal.strip());
            } else if (type == long.class || type == Long.class) {
                return Long.parseLong(literal.strip());
            } else if (type == float.class || type == Float.class) {
                return Float.parseFloat(literal.strip());
            } else if (type == double.class || type == Double.class) {
                return Double.parseDouble(literal.strip());
            } else if (type == boolean.class || type == Boolean.class) {
                return parseBoolean(literal);
            } else if (type.isEnum()) {
                return parseEnumConstant(type, literal);
            } else if (type == List.class && EMPTY_LIST.equals(literal.strip())) {
                return List.of();
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'%s' is not a valid %s".formatted(literal, type.getSimpleName()), e);
        }
        throw new IllegalArgumentException("A default of '%s' is not supported for %s".formatted(literal, type.getName()));
    }

    private static boolean isNumeric(Class<?> type) {
        return type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == float.class || type == Float.class
                || type == double.class || type == Double.class;
    }

    private static Boolean parseBoolean(String literal) {
        return switch (literal.strip()) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("'%s' is not a valid boolean".formatted(literal));
        };
    }

    private static String parseEnumConstant(Class<?> type, String literal) {
        for (var constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(literal)) {
                return literal;
            }
        }
        throw new IllegalArgumentException("'%s' is not a constant of %s".formatted(literal, type.getName()));
    }
}

package be.appify.prefab.core.util;

import java.util.Locale;
import java.util.Set;

/**
 * The languages of a full-text search, named after the built-in PostgreSQL text search configurations. The special
 * language {@value #NEUTRAL} has no stemming and no stop words.
 */
public final class FullTextLanguages {
    /** The language-neutral configuration, used when no language is configured. */
    public static final String NEUTRAL = "simple";

    /** The name of the Spring property holding the application-wide default language. */
    public static final String PROPERTY = "prefab.full-text.language";

    /** The name of the annotation processor option holding the application-wide default language. */
    public static final String OPTION = "prefab.fullText.language";

    private static final Set<String> POSTGRES_CONFIGURATIONS = Set.of(
            NEUTRAL, "arabic", "armenian", "basque", "catalan", "danish", "dutch", "english", "finnish", "french",
            "german", "greek", "hindi", "hungarian", "indonesian", "irish", "italian", "lithuanian", "nepali",
            "norwegian", "portuguese", "romanian", "russian", "serbian", "spanish", "swedish", "tamil", "turkish",
            "yiddish");

    private FullTextLanguages() {
    }

    /**
     * Whether the language is a built-in PostgreSQL text search configuration.
     *
     * @param language
     *         the language
     * @return true if PostgreSQL knows the configuration out of the box
     */
    public static boolean isKnown(String language) {
        return POSTGRES_CONFIGURATIONS.contains(language);
    }

    /**
     * The name of the language in MongoDB, where the language-neutral configuration is called {@code none}.
     *
     * @param language
     *         the language
     * @return the MongoDB text index language
     */
    public static String mongoNameOf(String language) {
        return NEUTRAL.equals(language) ? "none" : language.toLowerCase(Locale.ROOT);
    }
}

package be.appify.prefab.core.annotations.rest;

/**
 * Controls where in a field value the autocomplete query term is matched.
 *
 * @see Autocomplete#scanMode()
 */
public enum ScanMode {
    /**
     * Match values that <em>start with</em> the query term.
     * <p>Generates {@code LIKE 'term%'} for JDBC and {@code ^term} regex for MongoDB.</p>
     */
    PREFIX,

    /**
     * Match values that <em>contain</em> the query term anywhere.
     * <p>Generates {@code LIKE '%term%'} for JDBC and {@code term} regex for MongoDB.</p>
     */
    CONTAINS
}


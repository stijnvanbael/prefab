package be.appify.prefab.core.diagnostics;

/**
 * Thrown when a Prefab application is misconfigured in a way the developer has to fix.
 *
 * <p>It carries what is wrong and what to do about it, so that
 * {@link PrefabConfigurationFailureAnalyzer} can print a short report at startup instead of a bare stack trace. The
 * exception itself stays the cause of the failure, so the full stack trace remains available with {@code --debug}.</p>
 */
public class PrefabConfigurationException extends IllegalStateException {
    private final String description;
    private final String action;

    /**
     * Constructs a new PrefabConfigurationException.
     *
     * @param description
     *         what is wrong, naming the property or bean involved
     * @param action
     *         what the developer has to do to fix it
     */
    public PrefabConfigurationException(String description, String action) {
        super(description + " " + action);
        this.description = description;
        this.action = action;
    }

    /**
     * What is wrong.
     *
     * @return the description of the misconfiguration
     */
    public String description() {
        return description;
    }

    /**
     * What to do to fix it.
     *
     * @return the suggested action
     */
    public String action() {
        return action;
    }
}

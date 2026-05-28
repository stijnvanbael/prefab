package be.appify.prefab.core.annotations;

/**
 * Enum specifying where generated code from a plugin should be emitted.
 *
 * <p>Used by {@link Generate#target()} to control plugin output placement on a per-aggregate basis.
 */
public enum OutputTarget {
    /**
     * Use the plugin's default behavior. The plugin determines where code is generated
     * (typically {@code MAIN}).
     */
    DEFAULT,

    /**
     * Emit generated code to {@code src/main/java} (or equivalent in Gradle/Bazel).
     */
    MAIN,

    /**
     * Emit generated code to {@code src/test/java} (or equivalent in Gradle/Bazel).
     */
    TEST
}


package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
/**
 * Represents a single plugin override configuration.
 * Immutable record holding the override settings for a plugin on a specific aggregate.
 */
public record PluginOverride(
        Class<?> pluginClass,
        boolean enabled,
        OutputTarget target
) {
    /**
     * Creates a new PluginOverride.
     *
     * @param pluginClass the plugin class being configured
     * @param enabled whether the plugin is enabled
     * @param target the output target for generated code
     */
    public PluginOverride {
        if (pluginClass == null) {
            throw new IllegalArgumentException("pluginClass must not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
    }

    /**
     * Check if this override enables the plugin.
     *
     * @return true if the plugin should be enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the target output location for generated code.
     *
     * @return the output target
     */
    public OutputTarget outputTarget() {
        return target;
    }

    /**
     * Check if the target is DEFAULT (meaning the plugin chooses the location).
     *
     * @return true if using default target
     */
    public boolean isDefaultTarget() {
        return target == OutputTarget.DEFAULT;
    }
}


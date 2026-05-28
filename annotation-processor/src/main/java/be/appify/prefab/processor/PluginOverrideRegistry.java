package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
/**
 * Registry for per-aggregate plugin overrides read from {@code @Generate} annotations.
 *
 * <p>Stores {@link PluginOverride} configurations indexed by plugin class, enabling efficient
 * queries during code generation. Immutable after construction.
 */
public class PluginOverrideRegistry {
    private final Map<Class<?>, PluginOverride> overridesByPlugin;

    /**
     * Creates a new empty registry.
     */
    public PluginOverrideRegistry() {
        this(Collections.emptyMap());
    }

    /**
     * Creates a new registry with the given overrides.
     *
     * @param overridesByPlugin map of plugin class to override configuration
     */
    public PluginOverrideRegistry(Map<Class<?>, PluginOverride> overridesByPlugin) {
        this.overridesByPlugin = Map.copyOf(overridesByPlugin);
    }

    /**
     * Check whether a plugin is enabled for an aggregate.
     *
     * <p>Returns the override if one exists; otherwise assumes the plugin is enabled by default.
     *
     * @param pluginClass the plugin class to check
     * @return true if the plugin should be enabled
     */
    public boolean isPluginEnabled(Class<?> pluginClass) {
        PluginOverride override = findOverride(pluginClass);
        return override == null || override.isEnabled();
    }

    /**
     * Get the output target for a plugin's generated code.
     *
     * <p>Returns the override if one exists; otherwise returns {@code DEFAULT}.
     *
     * @param pluginClass the plugin class to check
     * @return the output target (never null)
     */
    public OutputTarget getOutputTarget(Class<?> pluginClass) {
        PluginOverride override = findOverride(pluginClass);
        return override == null ? OutputTarget.DEFAULT : override.outputTarget();
    }

    /**
     * Get the full override configuration for a plugin.
     *
     * @param pluginClass the plugin class to check
     * @return an Optional containing the override, or empty if no override exists
     */
    public Optional<PluginOverride> getOverride(Class<?> pluginClass) {
        return Optional.ofNullable(findOverride(pluginClass));
    }

    /**
     * Check if any plugins are explicitly disabled via overrides.
     *
     * @return true if at least one plugin has {@code enabled = false}
     */
    public boolean hasDisabledPlugins() {
        return overridesByPlugin.values().stream().anyMatch(o -> !o.isEnabled());
    }

    /**
     * Check if any plugins have a non-default target via overrides.
     *
     * @return true if at least one plugin has a non-DEFAULT target
     */
    public boolean hasNonDefaultTargets() {
        return overridesByPlugin.values().stream().anyMatch(o -> !o.isDefaultTarget());
    }

    private PluginOverride findOverride(Class<?> pluginClass) {
        var direct = overridesByPlugin.get(pluginClass);
        if (direct != null) {
            return direct;
        }
        var pluginClassName = pluginClass.getName();
        return overridesByPlugin.values().stream()
                .filter(override -> override.pluginClass().getName().equals(pluginClassName))
                .findFirst()
                .orElse(null);
    }
}



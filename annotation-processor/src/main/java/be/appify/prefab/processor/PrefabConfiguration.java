package be.appify.prefab.processor;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads annotation-processor options and exposes typed configuration queries.
 */
public final class PrefabConfiguration {
    private static final String PLUGIN_OPTION_PREFIX = "prefab.plugin.";
    private static final String ENABLED_OPTION_SUFFIX = ".enabled";

    private final Map<String, String> options;

    public PrefabConfiguration(Map<String, String> options) {
        this.options = Map.copyOf(options);
    }

    /**
     * Returns whether a plugin is enabled by project-wide compiler options.
     *
     * <p>Supported aliases (first match wins):
     * <ul>
     *   <li>{@code prefab.plugin.<fully-qualified-class>.enabled}</li>
     *   <li>{@code prefab.plugin.<simple-class>.enabled}</li>
     *   <li>{@code prefab.plugin.<simple-class-without-plugin-suffix-lowercase>.enabled}</li>
     *   <li>{@code prefab.plugin.<last-package-segment-lowercase>.enabled}</li>
     * </ul>
     */
    public boolean isPluginEnabled(Class<?> pluginClass) {
        return pluginOptionKeys(pluginClass).stream()
                .map(options::get)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .map(PrefabConfiguration::asBoolean)
                .orElse(true);
    }

    private static List<String> pluginOptionKeys(Class<?> pluginClass) {
        var simpleName = pluginClass.getSimpleName();
        var normalizedSimpleName = normalizePluginId(simpleName);
        var packageName = pluginClass.getPackageName();
        var packageSegment = packageName.substring(packageName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return List.of(
                optionKey(pluginClass.getName()),
                optionKey(simpleName),
                optionKey(normalizedSimpleName),
                optionKey(packageSegment)
        );
    }

    private static String optionKey(String pluginId) {
        return PLUGIN_OPTION_PREFIX + pluginId + ENABLED_OPTION_SUFFIX;
    }

    private static String normalizePluginId(String pluginSimpleName) {
        var withoutSuffix = pluginSimpleName.endsWith("Plugin")
                ? pluginSimpleName.substring(0, pluginSimpleName.length() - "Plugin".length())
                : pluginSimpleName;
        return withoutSuffix.toLowerCase(Locale.ROOT);
    }

    private static boolean asBoolean(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y", "on" -> true;
            case "false", "0", "no", "n", "off" -> false;
            default -> true;
        };
    }
}


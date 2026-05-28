package be.appify.prefab.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.appify.prefab.core.annotations.OutputTarget;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PluginOverrideRegistry}. */
@DisplayName("PluginOverrideRegistry")
class PluginOverrideRegistryTest {

    @Nested
    @DisplayName("with empty registry")
    class EmptyRegistryTests {

        @Test
        @DisplayName("should default all plugins to enabled")
        void enabledByDefault() {
            var registry = new PluginOverrideRegistry();

            assertTrue(registry.isPluginEnabled(MockPlugin.class));
            assertTrue(registry.isPluginEnabled(AnotherPlugin.class));
        }

        @Test
        @DisplayName("should default all plugins to DEFAULT output target")
        void defaultTargetByDefault() {
            var registry = new PluginOverrideRegistry();

            assertEquals(OutputTarget.DEFAULT, registry.getOutputTarget(MockPlugin.class));
            assertEquals(OutputTarget.DEFAULT, registry.getOutputTarget(AnotherPlugin.class));
        }

        @Test
        @DisplayName("should have no disabled plugins")
        void noDisabledPlugins() {
            var registry = new PluginOverrideRegistry();

            assertFalse(registry.hasDisabledPlugins());
        }

        @Test
        @DisplayName("should have no non-default targets")
        void noNonDefaultTargets() {
            var registry = new PluginOverrideRegistry();

            assertFalse(registry.hasNonDefaultTargets());
        }
    }

    @Nested
    @DisplayName("with configured overrides")
    class ConfiguredOverridesTests {

        @Test
        @DisplayName("should respect enabled = false override")
        void disabledPluginOverride() {
            var override = new PluginOverride(MockPlugin.class, false, OutputTarget.DEFAULT);
            @SuppressWarnings("unchecked")
            var map = (java.util.Map<Class<?>, PluginOverride>) (java.util.Map<?, ?>) java.util.Map.of(
                    MockPlugin.class, override
            );
            var registry = new PluginOverrideRegistry(map);

            assertFalse(registry.isPluginEnabled(MockPlugin.class));
            assertTrue(registry.isPluginEnabled(AnotherPlugin.class)); // Other plugins still enabled
        }

        @Test
        @DisplayName("should respect enabled = true override")
        void enabledPluginOverride() {
            var override = new PluginOverride(MockPlugin.class, true, OutputTarget.DEFAULT);
            var registry = new PluginOverrideRegistry(Map.of(MockPlugin.class, override));

            assertTrue(registry.isPluginEnabled(MockPlugin.class));
        }

        @Test
        @DisplayName("should respect OUTPUT_TARGET.TEST override")
        void testTargetOverride() {
            var override = new PluginOverride(MockPlugin.class, true, OutputTarget.TEST);
            var registry = new PluginOverrideRegistry(Map.of(MockPlugin.class, override));

            assertEquals(OutputTarget.TEST, registry.getOutputTarget(MockPlugin.class));
            assertEquals(OutputTarget.DEFAULT, registry.getOutputTarget(AnotherPlugin.class));
        }

        @Test
        @DisplayName("should respect OUTPUT_TARGET.MAIN override")
        void mainTargetOverride() {
            var override = new PluginOverride(MockPlugin.class, true, OutputTarget.MAIN);
            var registry = new PluginOverrideRegistry(Map.of(MockPlugin.class, override));

            assertEquals(OutputTarget.MAIN, registry.getOutputTarget(MockPlugin.class));
        }

        @Test
        @DisplayName("should detect disabled plugins")
        void detectsDisabledPlugins() {
            @SuppressWarnings("unchecked")
            var overrides = (java.util.Map<Class<?>, PluginOverride>) (java.util.Map<?, ?>) java.util.Map.of(
                    MockPlugin.class, new PluginOverride(MockPlugin.class, false, OutputTarget.DEFAULT),
                    AnotherPlugin.class, new PluginOverride(AnotherPlugin.class, true, OutputTarget.DEFAULT)
            );
            var registry = new PluginOverrideRegistry(overrides);

            assertTrue(registry.hasDisabledPlugins());
        }

        @Test
        @DisplayName("should detect non-default targets")
        void detectsNonDefaultTargets() {
            @SuppressWarnings("unchecked")
            var overrides = (java.util.Map<Class<?>, PluginOverride>) (java.util.Map<?, ?>) java.util.Map.of(
                    MockPlugin.class, new PluginOverride(MockPlugin.class, true, OutputTarget.TEST)
            );
            var registry = new PluginOverrideRegistry(overrides);

            assertTrue(registry.hasNonDefaultTargets());
        }

        @Test
        @DisplayName("should return empty Optional for unknown plugin")
        void unknownPluginReturnsEmpty() {
            var override = new PluginOverride(MockPlugin.class, false, OutputTarget.DEFAULT);
            var registry = new PluginOverrideRegistry(Map.of(MockPlugin.class, override));

            assertFalse(registry.getOverride(AnotherPlugin.class).isPresent());
        }

        @Test
        @DisplayName("should return non-empty Optional for configured plugin")
        void configuredPluginReturnsOverride() {
            var override = new PluginOverride(MockPlugin.class, false, OutputTarget.TEST);
            var registry = new PluginOverrideRegistry(Map.of(MockPlugin.class, override));

            assertTrue(registry.getOverride(MockPlugin.class).isPresent());
            assertEquals(override, registry.getOverride(MockPlugin.class).get());
        }
    }

    @Nested
    @DisplayName("with multiple overrides")
    class MultipleOverridesTests {

        @Test
        @DisplayName("should handle multiple plugins with different overrides")
        void multiplePluginsWithDifferentOverrides() {
            var overrides = new java.util.HashMap<Class<?>, PluginOverride>();
            overrides.put(MockPlugin.class, new PluginOverride(MockPlugin.class, false, OutputTarget.DEFAULT));
            overrides.put(AnotherPlugin.class, new PluginOverride(AnotherPlugin.class, true, OutputTarget.TEST));
            var registry = new PluginOverrideRegistry(overrides);

            assertFalse(registry.isPluginEnabled(MockPlugin.class));
            assertTrue(registry.isPluginEnabled(AnotherPlugin.class));
            assertEquals(OutputTarget.DEFAULT, registry.getOutputTarget(MockPlugin.class));
            assertEquals(OutputTarget.TEST, registry.getOutputTarget(AnotherPlugin.class));
        }
    }

    // Mock plugin classes for testing
    private static class MockPlugin implements PrefabPlugin {
    }

    private static class AnotherPlugin implements PrefabPlugin {
    }
}









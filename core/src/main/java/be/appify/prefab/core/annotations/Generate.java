package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Per-aggregate plugin override annotation. Repeatable; allows fine-grained control over code
 * generation behavior for a single Prefab aggregate.
 *
 * <p>Enables developers to:
 * <ul>
 *   <li>Enable or disable specific plugins on a per-aggregate basis, overriding project-wide
 *       settings
 *   <li>Control where generated code is emitted (main or test source trees)
 *   <li>Compose multiple overrides by repeating this annotation
 * </ul>
 *
 * <p><strong>Precedence:</strong> Per-aggregate overrides take strict precedence over
 * project-wide configuration (from compiler options in TASK-227):
 * <pre>
 * Override annotation (highest)
 * ↓
 * -Aprefab.plugin.X.enabled=... (project-wide default)
 * ↓
 * Default plugin behavior (lowest)
 * </pre>
 *
 * <p><strong>Usage examples:</strong>
 * <pre>
 * // Disable AsyncAPI documentation for this aggregate
 * // use test output for builders
 * &#64;Generate(plugin = AsyncApiDocumentationPlugin.class, enabled = false)
 * &#64;Generate(plugin = BuilderPlugin.class, target = OutputTarget.TEST)
 * &#64;Aggregate(root = "Order")
 * public class OrderAggregate {
 *     // ...
 * }
 *
 * // Force Kafka generation even if disabled globally
 * &#64;Generate(plugin = KafkaPlugin.class, enabled = true)
 * &#64;Aggregate(root = "Event")
 * public class EventAggregate {
 *     // ...
 * }
 *
 * // Custom migration scripts in test output
 * &#64;Generate(plugin = DbMigrationPlugin.class, target = OutputTarget.TEST)
 * &#64;Aggregate(root = "Account")
 * public class AccountAggregate {
 *     // ...
 * }
 * </pre>
 *
 * <p><strong>Validation:</strong> The annotation processor validates:
 * <ul>
 *   <li>The {@link #plugin()} value is a {@code PrefabPlugin} subclass (compile error if not)
 *   <li>The referenced plugin exists on the annotation-processor classpath (compile error if not)
 *   <li>The annotation is placed on a class with {@code @Aggregate} (warning if not)
 *   <li>The {@link #target()} is supported by the plugin (compile error if not)
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(GenerateOverrides.class)
public @interface Generate {
    /**
     * The plugin class to configure.
     *
     * <p>Must be a subclass of {@code PrefabPlugin}. If not, a compile error is raised
     * with a clear error message.
     *
     * <p><strong>Examples:</strong>
     * <ul>
     *   <li>{@code AsyncApiDocumentationPlugin.class}
     *   <li>{@code KafkaPlugin.class}
     *   <li>{@code DbMigrationPlugin.class}
     *   <li>{@code BuilderPlugin.class}
     * </ul>
     *
     * @return the plugin class to configure
     */
    Class<?> plugin();

    /**
     * Enable or disable this plugin for this aggregate.
     *
     * <p><strong>Semantics:</strong>
     * <ul>
     *   <li>{@code true} (default): Use default behavior; the plugin generates code as normal
     *   <li>{@code false}: The plugin is skipped entirely; no code generation hooks are invoked
     * </ul>
     *
     * <p><strong>Use cases:</strong>
     * <ul>
     *   <li>{@code enabled = false}: Disable AsyncAPI documentation for an aggregate that
     *       doesn't publish events yet
     *   <li>{@code enabled = true}: Force enable a plugin that is disabled globally via
     *       {@code -Aprefab.plugin.X.enabled=false}
     * </ul>
     *
     * @return {@code true} to enable the plugin, {@code false} to disable it; default {@code true}
     */
    boolean enabled() default true;

    /**
     * Where to emit generated code from this plugin.
     *
     * <p><strong>Values:</strong>
     * <ul>
     *   <li>{@code DEFAULT}: Use the plugin's default behavior (typically {@code MAIN})
     *   <li>{@code MAIN}: Emit to {@code src/main/java}
     *   <li>{@code TEST}: Emit to {@code src/test/java}
     * </ul>
     *
     * <p><strong>Validation:</strong> If the plugin does not support the specified target,
     * a compile error is raised. Plugins declare support via annotation or internal metadata.
     *
     * <p><strong>Use cases:</strong>
     * <ul>
     *   <li>{@code target = OutputTarget.TEST}: Generate test-only builders or fixtures
     *   <li>{@code target = OutputTarget.TEST}: Generate custom migration scripts in test resources
     *   <li>{@code target = OutputTarget.MAIN}: Explicitly force main source output (override defaults)
     * </ul>
     *
     * @return where to emit generated code; default {@code DEFAULT}
     */
    OutputTarget target() default OutputTarget.DEFAULT;
}



package be.appify.prefab.streams.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field of a test class for injection by {@link TopologyTestExtension}.
 *
 * <p>Supported field types are {@link be.appify.prefab.streams.PrefabStreams}, to declare the topology under test,
 * and {@link TopologySession}, to feed input and read output of that topology.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface TestStreams {
}

package be.appify.prefab.example.showcase.capability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a test class or method that only runs on variants supporting the given capabilities.
 *
 * <p>On a variant that lists one of them as unsupported in {@code showcase-capabilities.yml}, the test is skipped
 * with the variant, capability, reason and backlog task as skip message.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(CapabilityCondition.class)
public @interface RequiresCapability {

    /**
     * The capabilities the test needs.
     *
     * @return the required capabilities
     */
    Capability[] value();
}

package be.appify.prefab.example.showcase.capability;

import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Makes the capability gaps of a variant visible in every build.
 *
 * <p>Tests of capabilities whose code lives in optional source folders (streams, Avro) are not compiled on variants that
 * do not support them, so {@link RequiresCapability} cannot skip them. This emits one skipped test per unsupported
 * capability instead, with the reason and backlog task as message. It also fails the build when the capabilities file
 * of the variant is missing or invalid.</p>
 */
class CapabilityGapsTest {

    @TestFactory
    Stream<DynamicTest> unsupportedCapabilities() {
        var variant = VariantCapabilities.fromClasspath();
        return variant.gaps().stream()
                .map(gap -> dynamicTest(gap.capability().name(), () -> Assumptions.abort(variant.skipMessage(gap))));
    }
}

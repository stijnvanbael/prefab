package be.appify.prefab.example.showcase.capability;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Skips tests annotated with {@link RequiresCapability} on variants that do not support the capability.
 */
public class CapabilityCondition implements ExecutionCondition {

    private final Supplier<VariantCapabilities> capabilities;

    /** Reads the capabilities of the running variant from the test classpath. */
    public CapabilityCondition() {
        this(VariantCapabilities::fromClasspath);
    }

    CapabilityCondition(Supplier<VariantCapabilities> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return context.getElement().map(this::evaluate).orElseGet(CapabilityCondition::noRequirement);
    }

    ConditionEvaluationResult evaluate(AnnotatedElement element) {
        return AnnotationSupport.findAnnotation(element, RequiresCapability.class)
                .map(this::evaluate)
                .orElseGet(CapabilityCondition::noRequirement);
    }

    private static ConditionEvaluationResult noRequirement() {
        return ConditionEvaluationResult.enabled("No capability required");
    }

    private ConditionEvaluationResult evaluate(RequiresCapability requirement) {
        var variant = capabilities.get();
        var skipReasons = Arrays.stream(requirement.value())
                .flatMap(capability -> variant.gap(capability).stream())
                .map(variant::skipMessage)
                .collect(Collectors.joining("; "));
        return skipReasons.isEmpty()
                ? ConditionEvaluationResult.enabled("%s supports all required capabilities".formatted(variant.variant()))
                : ConditionEvaluationResult.disabled(skipReasons);
    }
}

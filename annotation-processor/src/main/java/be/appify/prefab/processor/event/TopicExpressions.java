package be.appify.prefab.processor.event;

/**
 * Helpers for topic values in event annotations that Spring resolves at runtime.
 */
public final class TopicExpressions {
    private TopicExpressions() {
    }

    /**
     * Returns whether the topic contains a property placeholder ({@code ${...}}) or a SpEL expression
     * ({@code #{...}}). Such topics must be injected with {@code @Value} rather than used literally. Because an
     * expression may expand to multiple topics (an array, a collection or a comma-separated string), generated code
     * injects it as a {@code String[]}.
     *
     * @param topic the topic value as declared in the annotation
     * @return {@code true} if the topic is resolved by Spring at runtime
     */
    public static boolean isExpression(String topic) {
        return topic.contains("${") || topic.contains("#{");
    }
}

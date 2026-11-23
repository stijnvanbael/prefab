package be.appify.prefab.processor.error;

import javax.lang.model.element.Element;

/**
 * Signals an <em>expected</em> failure: invalid user input, unusable resources or an infrastructure problem
 * (for example an unwritable output directory) that the user can fix.
 *
 * <p>Throw this from any processor code or {@link be.appify.prefab.processor.PrefabPlugin} instead of an unchecked
 * exception such as {@link IllegalStateException}. The nearest {@link ProcessingGuard} converts it into exactly one
 * {@code ERROR} diagnostic attached to {@link #element()} (or the element being processed when none is given),
 * containing the violated rule, the cause and the suggested corrective action. Anything that is <em>not</em> a
 * {@code PrefabProcessingException} is treated as an unexpected internal failure.
 */
public class PrefabProcessingException extends RuntimeException {
    private final transient Element element;
    private final String annotation;
    private final String ruleViolated;
    private final String suggestedFix;

    /**
     * Creates a failure without cause.
     *
     * @param element      the element to attach the diagnostic to, or {@code null} to use the element being processed
     * @param annotation   the annotation or subject tag shown in brackets, e.g. {@code "@Aggregate"}
     * @param ruleViolated what is wrong
     * @param suggestedFix how to fix it, may be {@code null}
     */
    public PrefabProcessingException(Element element, String annotation, String ruleViolated, String suggestedFix) {
        this(element, annotation, ruleViolated, suggestedFix, null);
    }

    /**
     * Creates a failure with a cause.
     *
     * @param element      the element to attach the diagnostic to, or {@code null} to use the element being processed
     * @param annotation   the annotation or subject tag shown in brackets, e.g. {@code "@Aggregate"}
     * @param ruleViolated what is wrong
     * @param suggestedFix how to fix it, may be {@code null}
     * @param cause        the underlying exception, may be {@code null}
     */
    public PrefabProcessingException(
            Element element,
            String annotation,
            String ruleViolated,
            String suggestedFix,
            Throwable cause
    ) {
        super(ruleViolated, cause);
        this.element = element;
        this.annotation = annotation;
        this.ruleViolated = ruleViolated;
        this.suggestedFix = suggestedFix;
    }

    /**
     * Returns the element the diagnostic is attached to.
     *
     * @return the element or {@code null} when unknown
     */
    public Element element() {
        return element;
    }

    /**
     * Returns the annotation or subject tag.
     *
     * @return the tag
     */
    public String annotation() {
        return annotation;
    }

    /**
     * Returns the violated rule.
     *
     * @return the rule description
     */
    public String ruleViolated() {
        return ruleViolated;
    }

    /**
     * Returns the corrective action.
     *
     * @return the suggestion or {@code null}
     */
    public String suggestedFix() {
        return suggestedFix;
    }
}

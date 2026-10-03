package be.appify.prefab.processor.error;

import java.util.function.Consumer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * Central error reporting utility for the annotation processor.
 *
 * <p>Provides a consistent interface for emitting validation errors with proper element attachment,
 * structured messages, and corrective action suggestions.
 *
 * <p>All messages are emitted via the Messager to ensure proper IDE integration and error highlighting.
 */
public class ErrorReporter {
    private final ProcessingEnvironment processingEnvironment;

    /**
     * Creates a new error reporter.
     *
     * @param processingEnvironment the annotation processor's environment
     */
    public ErrorReporter(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    /**
     * Reports a validation error with structured information.
     *
     * <p>Message format: {@code [annotation] element: rule violated. Suggested fix: corrective action}
     *
     * @param element the element where the error occurred
     * @param annotation the annotation that triggered validation (e.g., "@Aggregate")
     * @param ruleViolated the validation rule that was violated
     * @param suggestedFix the corrective action to resolve the error (may be null)
     */
    public void reportValidationError(
            Element element,
            String annotation,
            String ruleViolated,
            String suggestedFix
    ) {
        var message = formatMessage(annotation, element, ruleViolated, suggestedFix);
        printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * Reports a validation error without a suggestion.
     *
     * @param element the element where the error occurred
     * @param annotation the annotation that triggered validation
     * @param ruleViolated the validation rule that was violated
     */
    public void reportValidationError(
            Element element,
            String annotation,
            String ruleViolated
    ) {
        reportValidationError(element, annotation, ruleViolated, null);
    }

    /**
     * Reports a validation warning with structured information.
     *
     * @param element the element where the warning occurred
     * @param message the warning message
     */
    public void reportWarning(Element element, String message) {
        printMessage(Diagnostic.Kind.WARNING, message, element);
    }

    /**
     * Reports a note message.
     *
     * @param element the element associated with the note
     * @param message the note message
     */
    public void reportNote(Element element, String message) {
        printMessage(Diagnostic.Kind.NOTE, message, element);
    }

    /**
     * Reports an error with a custom formatted message.
     *
     * <p>Use this method when the error format doesn't fit the standard pattern.
     *
     * @param element the element where the error occurred
     * @param message the error message
     */
    public void reportError(Element element, String message) {
        printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * Collects multiple validation errors before reporting them all.
     *
     * <p>Returns a consumer that collects errors and a runnable that reports them all
     * at once when done.
     *
     * @return a tuple containing a consumer for errors and a runnable to report them
     */
    public ErrorCollector collectErrors() {
        return new ErrorCollector(this);
    }

    private String formatMessage(String annotation, Element element, String ruleViolated, String suggestedFix) {
        var sb = new StringBuilder();
        sb.append("[").append(annotation).append("] ");

        // Add element name if available
        if (element.getSimpleName() != null && !element.getSimpleName().toString().isEmpty()) {
            sb.append(element.getSimpleName()).append(": ");
        }

        sb.append(ruleViolated);

        if (suggestedFix != null && !suggestedFix.isBlank()) {
            sb.append("\nSuggested fix: ").append(suggestedFix);
        }

        return sb.toString();
    }

    private void printMessage(Diagnostic.Kind kind, String message, Element element) {
        processingEnvironment.getMessager().printMessage(kind, message, element);
    }

    /**
     * Helper class for collecting multiple validation errors.
     */
    public static class ErrorCollector {
        private final ErrorReporter reporter;
        private final java.util.List<ValidationError> errors = new java.util.ArrayList<>();

        ErrorCollector(ErrorReporter reporter) {
            this.reporter = reporter;
        }

        /**
         * Collects a validation error.
         *
         * @param element the element where the error occurred
         * @param annotation the annotation that triggered validation
         * @param ruleViolated the validation rule that was violated
         * @param suggestedFix the corrective action to resolve the error
         * @return this collector for chaining
         */
        public ErrorCollector error(Element element, String annotation, String ruleViolated, String suggestedFix) {
            errors.add(new ValidationError(element, annotation, ruleViolated, suggestedFix));
            return this;
        }

        /**
         * Collects a validation error without a suggestion.
         *
         * @param element the element where the error occurred
         * @param annotation the annotation that triggered validation
         * @param ruleViolated the validation rule that was violated
         * @return this collector for chaining
         */
        public ErrorCollector error(Element element, String annotation, String ruleViolated) {
            return error(element, annotation, ruleViolated, null);
        }

        /**
         * Reports all collected errors.
         */
        public void reportAll() {
            errors.forEach(e -> reporter.reportValidationError(e.element(), e.annotation(), e.ruleViolated(), e.suggestedFix()));
        }

        /**
         * Returns true if any errors were collected.
         *
         * @return true if there are errors
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /**
         * Gets the number of collected errors.
         *
         * @return the error count
         */
        public int errorCount() {
            return errors.size();
        }

        /**
         * Executes an action only if errors were collected.
         *
         * @param action the action to execute
         */
        public void ifHasErrors(Consumer<ErrorCollector> action) {
            if (hasErrors()) {
                action.accept(this);
            }
        }

        private record ValidationError(Element element, String annotation, String ruleViolated, String suggestedFix) {
        }
    }
}


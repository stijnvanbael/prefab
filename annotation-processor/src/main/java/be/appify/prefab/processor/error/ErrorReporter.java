package be.appify.prefab.processor.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
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
    /**
     * Compiler option ({@code -Aprefab.diagnostics.stacktrace=true}) that appends the full stack trace of
     * unexpected internal failures to the diagnostic.
     */
    public static final String STACKTRACE_OPTION = "prefab.diagnostics.stacktrace";

    // Errors already reported per compilation, so that the same failure reached through several code paths
    // (for example a malformed shared resource) is reported once.
    private static final Map<ProcessingEnvironment, Set<String>> REPORTED_ERRORS = new WeakHashMap<>();

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
     * Reports a validation finding as a warning, in the same format as {@link #reportValidationError}.
     *
     * @param element the element the warning is about (may be null)
     * @param annotation the annotation or component that triggered validation
     * @param ruleViolated the rule that was violated
     * @param suggestedFix the corrective action (may be null)
     */
    public void reportValidationWarning(
            Element element,
            String annotation,
            String ruleViolated,
            String suggestedFix
    ) {
        printMessage(Diagnostic.Kind.WARNING, formatMessage(annotation, element, ruleViolated, suggestedFix), element);
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
     * Reports a failure that escaped from processor or plugin code as exactly one {@code ERROR} diagnostic.
     *
     * <p>A {@link PrefabProcessingException} is an expected failure and is reported with its rule, cause and
     * suggested fix. Any other exception is an unexpected internal failure: the diagnostic names the failing
     * component and the cause chain, and includes the full stack trace when
     * {@code -A}{@value #STACKTRACE_OPTION}{@code =true} is set.
     *
     * @param component the processor or plugin that was running
     * @param fallbackElement the element being processed, used when the failure carries no element (may be null)
     * @param failure the failure to report
     */
    public void reportFailure(String component, Element fallbackElement, RuntimeException failure) {
        if (failure instanceof PrefabProcessingException expected) {
            var element = expected.element() != null ? expected.element() : fallbackElement;
            var message = new StringBuilder(formatMessage(expected.annotation(), element,
                    expected.ruleViolated(), expected.suggestedFix()));
            if (expected.getCause() != null) {
                message.append("\nCaused by: ").append(describe(expected.getCause()));
            }
            printMessage(Diagnostic.Kind.ERROR, message.toString(), element);
            return;
        }
        var message = new StringBuilder("[Prefab] Internal error in ").append(component);
        if (fallbackElement != null) {
            message.append(" while processing ").append(fallbackElement.getSimpleName());
        }
        message.append(": ").append(describe(failure));
        for (var cause = failure.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
            message.append("\nCaused by: ").append(describe(cause));
        }
        message.append("\nThis is a bug in ").append(component)
                .append(", not in your code. Please report it with this message");
        if (Boolean.parseBoolean(processingEnvironment.getOptions().get(STACKTRACE_OPTION))) {
            message.append("\n").append(stackTraceOf(failure));
        } else {
            message.append(". Re-run the compilation with -A").append(STACKTRACE_OPTION)
                    .append("=true for the full stack trace");
        }
        printMessage(Diagnostic.Kind.ERROR, message.toString(), fallbackElement);
    }

    private Set<String> reportedErrors() {
        synchronized (REPORTED_ERRORS) {
            return REPORTED_ERRORS.computeIfAbsent(processingEnvironment, key -> new HashSet<>());
        }
    }

    private static String describe(Throwable throwable) {
        return throwable.getMessage() == null
                ? throwable.getClass().getName()
                : throwable.getClass().getName() + ": " + throwable.getMessage();
    }

    private static String stackTraceOf(Throwable throwable) {
        var writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString().stripTrailing();
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
        if (element != null && element.getSimpleName() != null && !element.getSimpleName().toString().isEmpty()) {
            sb.append(element.getSimpleName()).append(": ");
        }

        sb.append(ruleViolated);

        if (suggestedFix != null && !suggestedFix.isBlank()) {
            sb.append("\nSuggested fix: ").append(suggestedFix);
        }

        return sb.toString();
    }

    private void printMessage(Diagnostic.Kind kind, String message, Element element) {
        if (kind == Diagnostic.Kind.ERROR && !reportedErrors().add(element + "|" + message)) {
            return;
        }
        if (element == null) {
            processingEnvironment.getMessager().printMessage(kind, message);
        } else {
            processingEnvironment.getMessager().printMessage(kind, message, element);
        }
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


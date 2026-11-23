package be.appify.prefab.processor.handwritten;

import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.error.ErrorReporter;
import java.util.Locale;
import javax.lang.model.element.Element;

/**
 * Reports hand-written code that duplicates what Prefab generates.
 *
 * <p>Findings about Java code are compiler errors by default, because the aim is a clear signal to correct course.
 * Findings about SQL migrations are warnings by default, because Prefab cannot tell a hand-written script from a
 * renamed copy of a generated one. Set {@code -Aprefab.handwritten.severity=error} or {@code =warning} to choose one
 * severity for all findings, for example while an existing application is being migrated to Prefab. Disable the
 * checks entirely with {@code -Aprefab.plugin.handwrittencode.enabled=false}.
 */
public final class HandWrittenFindings {
    /** Compiler option that selects the severity of all findings: {@code error} or {@code warning}. */
    public static final String SEVERITY_OPTION = "prefab.handwritten.severity";
    /** Label shown in front of every finding. */
    static final String LABEL = "Prefab";

    /** The severity of a finding. */
    public enum Severity {
        /** Fails the compilation. */
        ERROR,
        /** Reported, but the compilation succeeds. */
        WARNING
    }

    private final ErrorReporter reporter;
    private final Severity severity;

    private HandWrittenFindings(ErrorReporter reporter, Severity severity) {
        this.reporter = reporter;
        this.severity = severity;
    }

    /**
     * Creates the reporter for a compilation.
     *
     * @param context
     *         the processing context
     * @param defaultSeverity
     *         the severity used when the compilation does not set {@value #SEVERITY_OPTION}
     * @return a reporter honouring the configured severity
     */
    public static HandWrittenFindings of(PrefabContext context, Severity defaultSeverity) {
        var configured = context.processingEnvironment().getOptions().get(SEVERITY_OPTION);
        var severity = configured == null ? defaultSeverity : switch (configured.strip().toLowerCase(Locale.ROOT)) {
            case "warning" -> Severity.WARNING;
            case "error" -> Severity.ERROR;
            default -> defaultSeverity;
        };
        return new HandWrittenFindings(context.errorReporter(), severity);
    }

    /**
     * Reports one finding.
     *
     * @param element
     *         the offending element, or {@code null} when it is not a Java element (for example a SQL file)
     * @param problem
     *         what is duplicated and what Prefab already provides
     * @param suggestedFix
     *         the recommended alternative or extension point
     */
    public void report(Element element, String problem, String suggestedFix) {
        if (severity == Severity.WARNING) {
            reporter.reportValidationWarning(element, LABEL, problem, suggestedFix);
        } else {
            reporter.reportValidationError(element, LABEL, problem, suggestedFix);
        }
    }
}

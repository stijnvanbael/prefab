package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Decides whether an agent solved a task. Runs, in order: guardrails on the changed files, the read-only check for
 * {@code src/main} (test-writing tasks), the agent's own tests against its code and against every mutant, and finally
 * the hidden acceptance tests, which are copied in only after the agent has finished.
 */
final class AcceptanceRunner {
    static final String AGENT_TAG = "agent";
    private static final int MAX_BUILD_ERRORS = 25;
    private static final java.util.regex.Pattern SPRING_CONTEXT =
            java.util.regex.Pattern.compile("@(IntegrationTest|SpringBootTest|WebMvcTest|DataJdbcTest)\\b");
    private static final String DOCKER_MISSING = "Could not find a valid Docker environment";

    private final Maven maven;
    private final Path support;

    /**
     * @param maven   runs the builds
     * @param support test helpers copied into the workspace together with the hidden tests
     */
    AcceptanceRunner(Maven maven, Path support) {
        this.maven = maven;
        this.support = support;
    }

    /**
     * Checks a workspace after the agent has finished.
     *
     * @param task      the task
     * @param workspace project root
     * @param git       git handle of the workspace
     * @param logs      directory for build logs
     * @return the outcome of every check
     */
    AcceptanceResult check(BenchmarkTask task, Path workspace, Git git, Path logs) {
        var changes = git.changesSince(WorkspaceFactory.STARTER_TAG);
        var diff = git.diffSince(WorkspaceFactory.STARTER_TAG);
        FileTrees.writeString(logs.resolve("agent.diff"), diff);
        git.commitAll(AGENT_TAG);
        var checks = new ArrayList<CheckResult>();
        var violations = Guardrails.check(workspace, changes, task.guardrails());
        checks.add(guardrailCheck(violations));
        if (task.acceptance().mainReadOnly()) {
            checks.add(mainUnchanged(changes));
        }
        var log = logs.resolve("acceptance.log");
        if (task.acceptance().agentTests()) {
            checks.addAll(agentTestChecks(task, workspace, git, changes, log));
        }
        SurefireReports.TestSummary hiddenSummary = null;
        if (task.hasHiddenTests()) {
            git.resetTo(AGENT_TAG);
            hiddenSummary = runHiddenTests(task, workspace, log);
            checks.add(hiddenCheck(task, hiddenSummary, log));
        }
        return new AcceptanceResult(checks, violations, changes, hiddenSummary, buildErrors(log), dockerMissing(log));
    }

    private static CheckResult guardrailCheck(List<Guardrails.Violation> violations) {
        var failing = violations.stream().filter(violation -> violation.severity() == Guardrails.Severity.FAIL).toList();
        var detail = failing.isEmpty() ? "no blocking violations"
                : failing.stream().map(violation -> violation.rule() + " in " + violation.file())
                        .collect(Collectors.joining("; "));
        return new CheckResult("guardrails", failing.isEmpty(), detail, false);
    }

    private static CheckResult mainUnchanged(List<Git.ChangedFile> changes) {
        var changedMain = changes.stream().map(Git.ChangedFile::path).filter(path -> path.startsWith("src/main/")).toList();
        return new CheckResult("main-unchanged", changedMain.isEmpty(),
                changedMain.isEmpty() ? "src/main untouched" : "changed " + changedMain, false);
    }

    private List<CheckResult> agentTestChecks(
            BenchmarkTask task, Path workspace, Git git, List<Git.ChangedFile> changes, Path log) {
        var checks = new ArrayList<CheckResult>();
        var testFiles = changes.stream()
                .filter(change -> change.status() != 'D')
                .map(Git.ChangedFile::path)
                .filter(path -> path.startsWith("src/test/java/") && path.endsWith(".java"))
                .toList();
        checks.add(new CheckResult("agent-tests-written", !testFiles.isEmpty(),
                testFiles.isEmpty() ? "no test sources added" : testFiles.toString(), false));
        if (task.acceptance().unitTestsOnly()) {
            var integrationTests = testFiles.stream()
                    .filter(file -> SPRING_CONTEXT.matcher(FileTrees.readString(workspace.resolve(file))).find())
                    .toList();
            checks.add(new CheckResult("unit-tests-only", integrationTests.isEmpty(), integrationTests.isEmpty()
                    ? "no Spring context in the agent's tests" : "start a Spring context: " + integrationTests, false));
        }
        SurefireReports.clear(workspace);
        var build = maven.test(workspace, List.of(), log);
        var summary = SurefireReports.read(workspace, name -> true);
        var passed = build.succeeded() && summary.failed() == 0 && summary.tests() >= task.acceptance().minAgentTests();
        checks.add(new CheckResult("agent-tests-pass", passed,
                "%d tests, %d failed (need at least %d)%s".formatted(summary.tests(), summary.failed(),
                        task.acceptance().minAgentTests(), failureSuffix(summary)), false));
        for (var mutant : task.acceptance().mutants()) {
            if (summary.reportsFound() == 0) {
                checks.add(new CheckResult("mutant-killed:" + mutant, false, "no agent tests ran", false));
                continue;
            }
            git.resetTo(AGENT_TAG);
            FileTrees.copy(task.mutant(mutant), workspace);
            SurefireReports.clear(workspace);
            maven.test(workspace, List.of(), log);
            var mutantSummary = SurefireReports.read(workspace, name -> true);
            var compiled = mutantSummary.reportsFound() > 0;
            checks.add(new CheckResult("mutant-killed:" + mutant, compiled && mutantSummary.failed() > 0,
                    compiled ? "%d of %d tests failed on the mutant".formatted(mutantSummary.failed(), mutantSummary.tests())
                            : "tests did not run on the mutant (compile error); see acceptance.log",
                    !compiled));
        }
        git.resetTo(AGENT_TAG);
        return checks;
    }

    private SurefireReports.TestSummary runHiddenTests(BenchmarkTask task, Path workspace, Path log) {
        if (java.nio.file.Files.isDirectory(support)) {
            FileTrees.copy(support, workspace);
        }
        FileTrees.copy(task.hidden(), workspace);
        SurefireReports.clear(workspace);
        var classes = hiddenTestClasses(task);
        maven.test(workspace, classes.stream().toList(), log);
        Predicate<String> hidden = classes::contains;
        return SurefireReports.read(workspace, hidden);
    }

    private static CheckResult hiddenCheck(BenchmarkTask task, SurefireReports.TestSummary summary, Path log) {
        var expectedClasses = hiddenTestClasses(task).size();
        if (summary.reportsFound() == 0) {
            var docker = dockerMissing(log);
            return new CheckResult("hidden-tests", false,
                    docker ? "Docker is not available" : "hidden tests did not run (build or compile failure)", docker);
        }
        var passed = summary.allPassed() && summary.reportsFound() == expectedClasses;
        return new CheckResult("hidden-tests", passed, "%d/%d passed%s".formatted(
                summary.tests() - summary.failed() - summary.skipped(), summary.tests(), failureSuffix(summary)), false);
    }

    static Set<String> hiddenTestClasses(BenchmarkTask task) {
        var sources = task.hidden().resolve("src/test/java");
        return FileTrees.files(sources).stream()
                .map(file -> sources.relativize(file).toString().replace('\\', '/'))
                .filter(path -> path.endsWith("Test.java") || path.endsWith("Tests.java"))
                .map(path -> path.substring(0, path.length() - ".java".length()).replace('/', '.'))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String failureSuffix(SurefireReports.TestSummary summary) {
        return summary.failures().isEmpty() ? "" : "; " + String.join(" | ", summary.failures());
    }

    private static boolean dockerMissing(Path log) {
        return FileTrees.withLines(log, lines -> lines.anyMatch(line -> line.contains(DOCKER_MISSING)), false);
    }

    static List<String> buildErrors(Path log) {
        return FileTrees.withLines(log, lines -> lines
                .filter(line -> line.startsWith("[ERROR] /") || line.contains("error:")
                        || line.contains("Fatal error compiling")
                        || (line.startsWith("[ERROR]") && line.contains("Prefab")))
                .map(String::strip)
                .distinct()
                .limit(MAX_BUILD_ERRORS)
                .toList(), List.of());
    }

    /**
     * Outcome of one acceptance check.
     *
     * @param name           check name, e.g. {@code hidden-tests} or {@code mutant-killed:wrong-total}
     * @param passed         whether it passed
     * @param detail         human-readable evidence
     * @param infrastructure whether the failure points at the benchmark or its environment rather than the agent
     */
    record CheckResult(String name, boolean passed, String detail, boolean infrastructure) {
    }

    /**
     * Outcome of all acceptance checks.
     *
     * @param checks            every check, in order
     * @param violations        guardrail violations including warnings
     * @param changes           files the agent changed
     * @param hiddenTests       results of the hidden tests, {@code null} when the task has none
     * @param buildErrors       compiler and processor errors from the acceptance builds
     * @param dockerUnavailable whether Testcontainers could not reach Docker
     */
    record AcceptanceResult(
            List<CheckResult> checks,
            List<Guardrails.Violation> violations,
            List<Git.ChangedFile> changes,
            SurefireReports.TestSummary hiddenTests,
            List<String> buildErrors,
            boolean dockerUnavailable
    ) {
        AcceptanceResult {
            checks = List.copyOf(checks);
            violations = List.copyOf(violations);
            changes = List.copyOf(changes);
            buildErrors = List.copyOf(buildErrors);
        }

        boolean passed() {
            return checks.stream().allMatch(CheckResult::passed);
        }
    }
}

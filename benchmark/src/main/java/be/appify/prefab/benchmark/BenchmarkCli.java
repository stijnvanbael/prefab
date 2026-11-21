package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line entry point of the Prefab agent benchmark. Run it through {@code benchmark/run.sh}; see
 * {@code backlog/docs/benchmark.md} for the full guide.
 */
public final class BenchmarkCli {
    private static final DateTimeFormatter RUN_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);
    private static final String USAGE = """
            Prefab agent benchmark

            Usage: benchmark/run.sh <command> [options]

            Commands:
              run        Run tasks with an agent and write a report
                           --model <id>           model to test (required for --agent claude), e.g. claude-haiku-5-5
                           --context <name>       documentation setup from benchmark/contexts (default: none)
                           --agent <name>         claude (default), reference or noop
                           --tasks <globs>        comma-separated task ids or categories, globs allowed (default: all)
                           --repeat <n>           attempts per task (default: 1)
                           --parallel <n>         attempts running at the same time (default: 2)
                           --triage-model <id>    let this model classify failures (default: heuristics only)
                           --results <dir>        report directory (default: benchmark/results/<run-id>)
                           --work-dir <dir>       workspace directory (default: <tmp>/prefab-benchmark/<run-id>)
                           --claude <path>        Claude Code CLI executable (default: claude)
                           --hide <paths>         extra comma-separated paths to hide from the agent
                           --keep-workspaces      keep workspaces of passed attempts too
              validate   Check every task: the reference solution must pass, the starting project must fail
                           --tasks, --parallel, --work-dir as above
              list       List tasks and contexts
              compare    Compare runs: compare <report.json>... [--output <file.md>]
              classify   Re-render a report after editing its classifications.yml: classify <run-dir>
            """;

    private BenchmarkCli() {
    }

    /**
     * Runs a command.
     *
     * @param args command and options
     */
    public static void main(String... args) {
        try {
            var arguments = CliArguments.parse(args);
            var paths = BenchmarkPaths.discover(Path.of(arguments.option("repository", ".")));
            int exitCode = switch (arguments.command()) {
                case "run" -> run(paths, arguments);
                case "validate" -> validate(paths, arguments);
                case "list" -> list(paths);
                case "compare" -> compare(arguments);
                case "classify" -> classify(arguments);
                default -> {
                    System.out.println(USAGE);
                    yield "help".equals(arguments.command()) ? 0 : 2;
                }
            };
            System.exit(exitCode);
        } catch (IllegalArgumentException e) {
            System.err.println("error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static int run(BenchmarkPaths paths, CliArguments arguments) {
        var agentName = arguments.option("agent", "claude");
        var claude = arguments.option("claude", "claude");
        var startedAt = Instant.now();
        var context = ContextSetup.load(paths.contexts(), arguments.option("context", "none"));
        var model = arguments.option("model").orElse(agentName);
        var runId = RUN_ID_TIME.format(startedAt) + "-" + sanitize(model) + "-" + context.name();
        var workDirectory = workDirectory(arguments, runId);
        var isolation = isolation(paths, arguments, workDirectory);
        var agent = switch (agentName) {
            case "claude" -> new ClaudeCodeAgent(
                    arguments.option("model").orElseThrow(() -> new IllegalArgumentException("--model is required")),
                    claude, List.of(), ClaudeCodeAgent.forbiddenMarkers(paths.repository()), isolation);
            case "reference" -> new ReferenceAgent();
            case "noop" -> new NoopAgent();
            default -> throw new IllegalArgumentException("Unknown agent " + agentName);
        };
        var tasks = new TaskCatalog(paths.tasks()).select(arguments.listOption("tasks"));
        var triage = arguments.option("triage-model").map(triageModel -> new Triage(triageModel, claude)).orElse(null);
        if (agent instanceof ClaudeCodeAgent && !isolation.enabled()) {
            System.out.println("warning: cannot hide the repository from the agent on this machine (needs Linux, root"
                    + " and unshare); attempts that read benchmark sources are marked contaminated");
        }
        var resultsDirectory = Path.of(arguments.option("results", paths.results().resolve(runId).toString()));
        var options = new BenchmarkRunner.Options(arguments.intOption("repeat", 1), arguments.intOption("parallel", 2),
                triage, arguments.flag("keep-workspaces"));
        var report = runner(paths, options).run(tasks, agent, context,
                runInfo(paths, runId, startedAt, resultsDirectory, workDirectory));
        writeReport(report, resultsDirectory);
        System.out.printf("%nPass rate %d/%d = %s%nReport: %s%n", report.passedCount(), report.results().size(),
                report.passRate().format(), resultsDirectory.resolve("report.md"));
        return 0;
    }

    private static int validate(BenchmarkPaths paths, CliArguments arguments) {
        var tasks = new TaskCatalog(paths.tasks()).select(arguments.listOption("tasks"));
        var context = ContextSetup.load(paths.contexts(), "none");
        var options = new BenchmarkRunner.Options(1, arguments.intOption("parallel", 2), null, false);
        var startedAt = Instant.now();
        var problems = new ArrayList<String>();
        for (Agent agent : List.of(new ReferenceAgent(), new NoopAgent())) {
            var runId = "validate-" + RUN_ID_TIME.format(startedAt) + "-" + agent.name();
            var work = workDirectory(arguments, runId);
            var report = runner(paths, options).run(tasks, agent, context,
                    runInfo(paths, runId, startedAt, work.resolve("results"), work.resolve("workspaces")));
            writeReport(report, work.resolve("results"));
            var expectPass = agent instanceof ReferenceAgent;
            report.results().stream()
                    .filter(result -> result.passed() != expectPass)
                    .forEach(result -> problems.add("%s: %s %s (%s)".formatted(result.taskId(), agent.name(),
                            expectPass ? "fails" : "passes", result.logs())));
        }
        if (problems.isEmpty()) {
            System.out.printf("%nAll %d tasks are valid: references pass, starting projects fail.%n", tasks.size());
            return 0;
        }
        System.out.printf("%n%d problems:%n", problems.size());
        problems.forEach(problem -> System.out.println("  " + problem));
        return 1;
    }

    private static int list(BenchmarkPaths paths) {
        var tasks = new TaskCatalog(paths.tasks()).all();
        System.out.printf("%d tasks:%n", tasks.size());
        tasks.forEach(task -> System.out.printf("  %-40s %-18s %s%n", task.id(), task.category(), task.title()));
        System.out.println("\nContexts:");
        ContextSetup.available(paths.contexts()).forEach(name -> System.out.printf("  %-20s %s%n", name,
                ContextSetup.load(paths.contexts(), name).description()));
        return 0;
    }

    private static int compare(CliArguments arguments) {
        if (arguments.positional().isEmpty()) {
            throw new IllegalArgumentException("compare needs one or more report.json files");
        }
        var reports = arguments.positional().stream().map(Path::of).map(RunReport::read).toList();
        var markdown = MarkdownReport.compare(reports);
        arguments.option("output").ifPresentOrElse(
                output -> FileTrees.writeString(Path.of(output), markdown),
                () -> System.out.println(markdown));
        return 0;
    }

    private static int classify(CliArguments arguments) {
        if (arguments.positional().size() != 1) {
            throw new IllegalArgumentException("classify needs a run directory");
        }
        var directory = Path.of(arguments.positional().getFirst());
        writeReport(RunReport.read(directory.resolve("report.json")), directory);
        System.out.println("Updated " + directory.resolve("report.md"));
        return 0;
    }

    private static void writeReport(RunReport report, Path directory) {
        var classified = ClassificationOverrides.apply(report, directory);
        classified.write(directory.resolve("report.json"));
        FileTrees.writeString(directory.resolve("report.md"), MarkdownReport.render(classified));
    }

    private static BenchmarkRunner runner(BenchmarkPaths paths, BenchmarkRunner.Options options) {
        return new BenchmarkRunner(paths, ProjectVersions.read(paths.repository()), options);
    }

    private static BenchmarkRunner.RunInfo runInfo(BenchmarkPaths paths, String runId, Instant startedAt,
            Path resultsDirectory, Path workDirectory) {
        var versions = ProjectVersions.read(paths.repository());
        var commit = new Git(paths.repository(), workDirectory.resolve("repository-git.log")).describeHead();
        return new BenchmarkRunner.RunInfo(runId, startedAt, resultsDirectory, workDirectory, versions.prefab(), commit);
    }

    /**
     * Hides the repository, the parent of this run's work directory (other runs' workspaces) and any {@code --hide}
     * paths. The default work directory lives in its own {@code <tmp>/prefab-benchmark} folder for this reason.
     */
    private static Isolation isolation(BenchmarkPaths paths, CliArguments arguments, Path workDirectory) {
        var hidden = new ArrayList<Path>(List.of(paths.repository(), workDirectory.toAbsolutePath().getParent()));
        arguments.listOption("hide").stream().map(Path::of).forEach(hidden::add);
        return Isolation.hiding(hidden);
    }

    private static Path workDirectory(CliArguments arguments, String runId) {
        return arguments.option("work-dir").map(Path::of)
                .orElse(Path.of(System.getProperty("java.io.tmpdir"), "prefab-benchmark"))
                .resolve(runId);
    }

    private static String sanitize(String text) {
        return text.replaceAll("[^A-Za-z0-9.-]+", "-");
    }
}

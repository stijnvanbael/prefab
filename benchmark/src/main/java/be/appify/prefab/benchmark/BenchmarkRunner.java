package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs a set of tasks with one agent and one context setup, in parallel slots, and collects a {@link RunReport}.
 *
 * <p>Every slot uses its own Testcontainers containers and Docker network (through the {@code prefab.test.*} name
 * properties, passed as environment variables so the agent's own builds use them too), so parallel attempts never
 * share a database or Kafka topic.
 */
final class BenchmarkRunner {
    private static final String WORKSPACE = "project";

    private final BenchmarkPaths paths;
    private final WorkspaceFactory workspaces;
    private final Options options;

    BenchmarkRunner(BenchmarkPaths paths, ProjectVersions versions, Options options) {
        this.paths = paths;
        this.workspaces = new WorkspaceFactory(paths, versions);
        this.options = options;
    }

    /**
     * Runs every task {@code repeat} times.
     *
     * @param tasks   the tasks
     * @param agent   the agent under test
     * @param context the documentation setup
     * @param run     identification of the run and where its results go
     * @return the report, with results sorted by task and attempt
     */
    RunReport run(List<BenchmarkTask> tasks, Agent agent, ContextSetup context, RunInfo run) {
        var attempts = new ArrayList<Attempt>();
        for (int attempt = 1; attempt <= options.repeat(); attempt++) {
            for (var task : tasks) {
                attempts.add(new Attempt(task, attempt));
            }
        }
        return runAttempts(attempts, agent, context, run);
    }

    /**
     * Runs specific attempts, for example the ones a rate limit interrupted.
     *
     * @param attempts which task attempts to run
     * @param agent    the agent under test
     * @param context  the documentation setup
     * @param run      identification of the run and where its results go
     * @return the report with only these attempts
     */
    RunReport runAttempts(List<Attempt> attempts, Agent agent, ContextSetup context, RunInfo run) {
        BlockingQueue<Integer> slots = new ArrayBlockingQueue<>(options.parallel());
        for (int slot = 1; slot <= options.parallel(); slot++) {
            slots.add(slot);
        }
        var preamble = FileTrees.readString(paths.agentPreamble()).strip();
        var futures = new ArrayList<Future<TaskResult>>();
        // Unique per invocation, so a resumed run never shares containers with processes of an earlier invocation.
        var runTag = Integer.toHexString((run.runId() + System.nanoTime()).hashCode() & 0xfffff);
        var rateLimited = new java.util.concurrent.atomic.AtomicBoolean();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var planned : attempts) {
                var task = planned.task();
                var attemptNumber = planned.number();
                futures.add(executor.submit(() -> {
                    var slot = slots.take();
                    try {
                        if (rateLimited.get()) {
                            return skipped(task, attemptNumber, run);
                        }
                        var result = attempt(task, attemptNumber, agent, context, preamble, run,
                                slotEnvironment(runTag, slot));
                        if (result.agent().status() == Agent.Status.RATE_LIMITED && !rateLimited.getAndSet(true)) {
                            log("STOP   the model API reports a usage or rate limit: %s", result.agent().message());
                        }
                        return result;
                    } finally {
                        slots.add(slot);
                    }
                }));
            }
        }
        for (int slot = 1; slot <= options.parallel(); slot++) {
            removeContainers(slotEnvironment(runTag, slot), run.directory());
        }
        var results = futures.stream().map(BenchmarkRunner::get)
                .sorted(Comparator.comparing(TaskResult::taskId).thenComparing(TaskResult::attempt))
                .toList();
        return new RunReport(run.runId(), run.startedAt(), agent.name(), agent.model(), context.name(),
                run.prefabVersion(), run.gitCommit(), options.repeat(), results);
    }

    private TaskResult attempt(BenchmarkTask task, int attempt, Agent agent, ContextSetup context, String preamble,
            RunInfo run, Map<String, String> environment) {
        var name = task.id() + "-" + attempt;
        var logs = run.directory().resolve("attempts").resolve(name);
        var workspace = run.workDirectory().resolve(name).resolve(WORKSPACE);
        log("start  %s", name);
        // A resumed attempt starts over: logs and transcripts of an earlier, interrupted try must not count.
        FileTrees.delete(logs);
        Agent.AgentOutcome outcome = null;
        try {
            var git = workspaces.create(task, context, workspace, logs.resolve("git.log"));
            resetContainers(environment, logs);
            outcome = agent.run(new Agent.AgentRun(task, preamble + "\n\n" + task.prompt(), workspace, logs, environment));
            ProcessReaper.killProcessesIn(workspace);
            if (outcome.status() == Agent.Status.RATE_LIMITED) {
                log("FAIL   %s rate limited", name);
                return new TaskResult(task.id(), task.category(), task.tier(), attempt, false, outcome,
                        new AcceptanceRunner.AcceptanceResult(List.of(new AcceptanceRunner.CheckResult("agent", false,
                                outcome.message(), true)), List.of(), List.of(), null, List.of(), false),
                        Duration.ZERO, new TaskResult.Classification(FailureCategory.TEST_OR_INFRASTRUCTURE, "heuristic",
                        "rate limited: " + outcome.message(), List.of()), logs.toString());
            }
            resetContainers(environment, logs);
            var acceptanceStart = Instant.now();
            var acceptance = new AcceptanceRunner(Maven.create(environment), paths.acceptanceSupport()).check(task, workspace, git, logs);
            var acceptanceDuration = Duration.between(acceptanceStart, Instant.now());
            var passed = acceptance.passed() && !outcome.contaminated();
            var result = new TaskResult(task.id(), task.category(), task.tier(), attempt, passed, outcome, acceptance,
                    acceptanceDuration, null, logs.toString());
            if (!passed) {
                result = result.withFailure(outcome.contaminated()
                        ? new TaskResult.Classification(FailureCategory.TEST_OR_INFRASTRUCTURE, "heuristic",
                        "the agent read the benchmark sources (hidden tests or reference)",
                        FailureClassifier.evidence(outcome, acceptance))
                        : FailureClassifier.classify(task, outcome, acceptance));
                if (options.triage() != null && result.failure().category() != FailureCategory.TEST_OR_INFRASTRUCTURE) {
                    result = options.triage().classify(task, result, logs);
                }
            } else if (!options.keepWorkspaces()) {
                FileTrees.delete(workspace.getParent());
            }
            log("%s %s %s", passed ? "PASS  " : "FAIL  ", name,
                    passed ? "" : result.failure().category() + " " + checkSummary(acceptance));
            return result;
        } catch (RuntimeException e) {
            log("ERROR  %s %s", name, e);
            var failedOutcome = outcome != null ? outcome : new Agent.AgentOutcome(Agent.Status.ERROR,
                    Agent.TokenUsage.NONE, 0, 0, 0, Duration.ZERO, false, String.valueOf(e.getMessage()));
            var acceptance = new AcceptanceRunner.AcceptanceResult(
                    List.of(new AcceptanceRunner.CheckResult("harness", false, e.toString(), true)),
                    List.of(), List.of(), null, List.of(), false);
            return new TaskResult(task.id(), task.category(), task.tier(), attempt, false, failedOutcome, acceptance,
                    Duration.ZERO, new TaskResult.Classification(FailureCategory.TEST_OR_INFRASTRUCTURE, "heuristic",
                            "the harness failed: " + e.getMessage(), List.of(e.toString())),
                    logs.toString());
        }
    }

    /**
     * Container and network names for a parallel slot. The run tag keeps concurrent benchmark runs on one machine
     * apart.
     */
    private static TaskResult skipped(BenchmarkTask task, int attempt, RunInfo run) {
        var outcome = new Agent.AgentOutcome(Agent.Status.SKIPPED, Agent.TokenUsage.NONE, 0, 0, 0, Duration.ZERO, false,
                "not started: an earlier attempt hit a usage or rate limit");
        var acceptance = new AcceptanceRunner.AcceptanceResult(
                List.of(new AcceptanceRunner.CheckResult("agent", false, outcome.message(), true)),
                List.of(), List.of(), null, List.of(), false);
        return new TaskResult(task.id(), task.category(), task.tier(), attempt, false, outcome, acceptance,
                Duration.ZERO, new TaskResult.Classification(FailureCategory.TEST_OR_INFRASTRUCTURE, "heuristic",
                outcome.message(), List.of()),
                run.directory().resolve("attempts").resolve(task.id() + "-" + attempt).toString());
    }

    static Map<String, String> slotEnvironment(String runTag, int slot) {
        var prefix = "prefab_bench_" + runTag + "_" + slot + "_";
        return Map.of(
                // No reuse: Testcontainers matches reusable containers by a configuration hash that ignores the
                // container name, so parallel slots would share (and remove) each other's databases.
                "TESTCONTAINERS_REUSE_ENABLE", "false",
                "PREFAB_TEST_POSTGRES_CONTAINER_NAME", prefix + "postgres",
                "PREFAB_TEST_MONGODB_CONTAINER_NAME", prefix + "mongodb",
                "PREFAB_TEST_KAFKA_CONTAINER_NAME", prefix + "kafka",
                "PREFAB_TEST_KAFKA_NETWORK_NAME", prefix + "kafka",
                "PREFAB_TEST_SCHEMA_REGISTRY_CONTAINER_NAME", prefix + "schema_registry",
                "PREFAB_TEST_LOCALSTACK_CONTAINER_NAME", prefix + "localstack",
                "PREFAB_TEST_PUBSUB_CONTAINER_NAME", prefix + "pubsub");
    }

    /**
     * Removes the slot's Testcontainers containers, so an attempt never sees topics, queues or tables left behind by an
     * earlier attempt (or by the agent's own test runs, for the acceptance builds).
     */
    private static void resetContainers(Map<String, String> environment, Path logs) {
        var command = new ArrayList<String>(List.of("docker", "rm", "--force", "--volumes"));
        environment.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("_CONTAINER_NAME"))
                .map(Map.Entry::getValue)
                .sorted()
                .forEach(command::add);
        ProcessRunner.run(command, ProcessRunner.ProcessOptions.of(logs, logs.resolve("docker.log"), Duration.ofMinutes(2)));
    }

    private static void removeContainers(Map<String, String> environment, Path directory) {
        FileTrees.append(directory.resolve("docker.log"), "");
        resetContainers(environment, directory);
        var network = environment.get("PREFAB_TEST_KAFKA_NETWORK_NAME");
        ProcessRunner.run(List.of("docker", "network", "rm", network),
                ProcessRunner.ProcessOptions.of(directory, directory.resolve("docker.log"), Duration.ofMinutes(1)));
    }

    private static String checkSummary(AcceptanceRunner.AcceptanceResult acceptance) {
        return acceptance.checks().stream().filter(check -> !check.passed())
                .map(AcceptanceRunner.CheckResult::name).toList().toString();
    }

    private static TaskResult get(Future<TaskResult> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Attempt failed unexpectedly", e.getCause());
        }
    }

    private static void log(String format, Object... args) {
        System.out.printf("[%tT] %s%n", Instant.now().toEpochMilli(), format.formatted(args));
    }

    /**
     * One planned attempt.
     *
     * @param task   the task
     * @param number 1-based attempt number
     */
    record Attempt(BenchmarkTask task, int number) {
    }

    /**
     * Run options.
     *
     * @param repeat         attempts per task
     * @param parallel       number of attempts running at the same time
     * @param triage         LLM triage for failures, or {@code null}
     * @param keepWorkspaces whether to keep the workspaces of passed attempts (failed ones are always kept)
     */
    record Options(int repeat, int parallel, Triage triage, boolean keepWorkspaces) {
    }

    /**
     * Identification of a run.
     *
     * @param runId         run id
     * @param startedAt     start time
     * @param directory     results directory (report, logs)
     * @param workDirectory directory for workspaces, outside the repository
     * @param prefabVersion Prefab version
     * @param gitCommit     repository commit
     */
    record RunInfo(String runId, Instant startedAt, Path directory, Path workDirectory, String prefabVersion,
            String gitCommit) {
    }
}

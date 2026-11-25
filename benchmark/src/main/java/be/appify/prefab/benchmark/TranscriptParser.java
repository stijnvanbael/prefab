package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Extracts run metrics from a Claude Code {@code --output-format stream-json} transcript.
 *
 * <p>The final {@code result} event carries the authoritative totals. When the run was killed before that event,
 * usage is summed from the assistant messages instead (deduplicated by message id).
 *
 * <p>The stream carries no timestamps. {@link ClaudeCodeAgent} records when each line arrived in a sidecar file
 * ({@link #timesFile}), one elapsed-milliseconds value per transcript line; with it, the parser reports when the first
 * Maven build the agent ran succeeded.
 */
final class TranscriptParser {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern MAVEN = Pattern.compile("(^|[\\s;&|(/])(mvn|mvnw)(\\s|$)");
    /** Output that shows a Maven build failed even when the tool reported success, e.g. behind {@code | tail}. */
    private static final Pattern BUILD_FAILED = Pattern.compile(
            "BUILD FAILURE|COMPILATION ERROR|Tests run: \\d+, Failures: [1-9]"
                    + "|Tests run: \\d+, Failures: \\d+, Errors: [1-9]");

    private TranscriptParser() {
    }

    /**
     * Parses a transcript without arrival times.
     *
     * @param lines            transcript lines (one JSON event per line)
     * @param forbiddenMarkers strings that must not appear in tool inputs (paths to benchmark sources)
     * @return the metrics
     */
    static Transcript parse(List<String> lines, List<String> forbiddenMarkers) {
        return parse(lines, List.of(), forbiddenMarkers);
    }

    /**
     * Parses a transcript.
     *
     * @param lines            transcript lines (one JSON event per line)
     * @param times            milliseconds since the start of the run at which each line arrived; ignored unless there
     *                         is exactly one per line
     * @param forbiddenMarkers strings that must not appear in tool inputs (paths to benchmark sources)
     * @return the metrics
     */
    static Transcript parse(List<String> lines, List<Long> times, List<String> forbiddenMarkers) {
        var timed = times.size() == lines.size();
        var mavenCalls = new HashSet<String>();
        Optional<Duration> firstGreenBuild = Optional.empty();
        var seenMessages = new HashSet<String>();
        var usage = Agent.TokenUsage.NONE;
        int assistantTurns = 0;
        int buildAttempts = 0;
        boolean contaminated = false;
        var results = new ArrayList<JsonNode>();
        for (int index = 0; index < lines.size(); index++) {
            var line = lines.get(index);
            var event = readEvent(line);
            if (event == null) {
                continue;
            }
            switch (event.path("type").asString("")) {
                case "assistant" -> {
                    var message = event.path("message");
                    if (seenMessages.add(message.path("id").asString(line))) {
                        usage = usage.plus(usage(message.path("usage")));
                        assistantTurns++;
                    }
                    for (var block : message.path("content")) {
                        if ("tool_use".equals(block.path("type").asString(""))) {
                            var input = block.path("input").toString();
                            contaminated |= forbiddenMarkers.stream().anyMatch(input::contains);
                            if ("Bash".equals(block.path("name").asString(""))
                                    && MAVEN.matcher(block.path("input").path("command").asString("")).find()) {
                                buildAttempts++;
                                mavenCalls.add(block.path("id").asString(""));
                            }
                        }
                    }
                }
                case "user" -> {
                    if (timed && firstGreenBuild.isEmpty() && succeededBuild(event.path("message"), mavenCalls)) {
                        firstGreenBuild = Optional.of(Duration.ofMillis(times.get(index)));
                    }
                }
                case "result" -> results.add(event);
                default -> {
                }
            }
        }
        if (results.isEmpty()) {
            return new Transcript(false, null, usage, assistantTurns, buildAttempts, 0, contaminated, "",
                    firstGreenBuild);
        }
        // Headless Claude Code emits one result per query. An agent that started background work keeps answering
        // its notifications after the main answer, so a transcript can hold many results: turns and tokens are
        // per query and add up, the cost is cumulative, and the main answer is the first non-empty one.
        var last = results.getLast();
        var totalUsage = results.stream().map(result -> usage(result.path("usage")))
                .reduce(Agent.TokenUsage.NONE, Agent.TokenUsage::plus);
        var turns = results.stream().mapToInt(result -> result.path("num_turns").asInt(0)).sum();
        var limited = results.stream().anyMatch(result -> result.path("api_error_status").asInt(0) == 429);
        var message = results.stream().map(result -> result.path("result").asString(""))
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElse(last.path("subtype").asString(""));
        return new Transcript(
                true,
                limited ? Agent.Status.RATE_LIMITED : status(last),
                totalUsage,
                turns == 0 ? assistantTurns : turns,
                buildAttempts,
                last.path("total_cost_usd").asDouble(0),
                contaminated,
                message,
                firstGreenBuild);
    }

    /** Parses a transcript file, with the arrival times from its {@link #timesFile} when there is one. */
    static Transcript parse(Path transcript, List<String> forbiddenMarkers) {
        try {
            var lines = Files.exists(transcript) ? Files.readAllLines(transcript) : List.<String>of();
            var timesFile = timesFile(transcript);
            var times = Files.exists(timesFile)
                    ? Files.readAllLines(timesFile).stream().filter(line -> !line.isBlank()).map(Long::parseLong).toList()
                    : List.<Long>of();
            return parse(lines, times, forbiddenMarkers);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + transcript, e);
        }
    }

    /** The sidecar file with the arrival time of each transcript line. */
    static Path timesFile(Path transcript) {
        return transcript.resolveSibling(transcript.getFileName().toString().replaceFirst("\\.jsonl$", "") + ".times");
    }

    /** Whether a tool-result message answers one of the given Maven calls with a successful build. */
    private static boolean succeededBuild(JsonNode message, Set<String> mavenCalls) {
        for (var block : message.path("content")) {
            if ("tool_result".equals(block.path("type").asString(""))
                    && mavenCalls.contains(block.path("tool_use_id").asString(""))
                    && !block.path("is_error").asBoolean(false)
                    && !BUILD_FAILED.matcher(block.path("content").toString()).find()) {
                return true;
            }
        }
        return false;
    }

    private static Agent.Status status(JsonNode result) {
        if (result.path("api_error_status").asInt(0) == 429) {
            return Agent.Status.RATE_LIMITED;
        }
        var subtype = result.path("subtype").asString("");
        if (subtype.equals("error_max_turns")) {
            return Agent.Status.MAX_TURNS;
        }
        if (subtype.equals("success") && !result.path("is_error").asBoolean(false)) {
            return Agent.Status.COMPLETED;
        }
        return Agent.Status.ERROR;
    }

    private static Agent.TokenUsage usage(JsonNode usage) {
        return new Agent.TokenUsage(
                usage.path("input_tokens").asLong(0),
                usage.path("output_tokens").asLong(0),
                usage.path("cache_read_input_tokens").asLong(0),
                usage.path("cache_creation_input_tokens").asLong(0));
    }

    private static JsonNode readEvent(String line) {
        if (line.isBlank() || !line.startsWith("{")) {
            return null;
        }
        try {
            return JSON.readTree(line);
        } catch (JacksonException e) {
            return null;
        }
    }

    /**
     * Metrics read from a transcript.
     *
     * @param complete      whether the transcript ends with a result event
     * @param status        how the run ended according to the agent, {@code null} when incomplete
     * @param usage         token usage
     * @param turns         number of turns
     * @param buildAttempts number of Maven invocations
     * @param costUsd       reported cost
     * @param contaminated  whether a tool input referenced the benchmark sources
     * @param finalMessage  the agent's final message
     * @param firstGreenBuild time until the first successful Maven build; empty when there was none or the arrival
     *                      times are unknown
     */
    record Transcript(
            boolean complete,
            Agent.Status status,
            Agent.TokenUsage usage,
            int turns,
            int buildAttempts,
            double costUsd,
            boolean contaminated,
            String finalMessage,
            Optional<Duration> firstGreenBuild
    ) {
    }
}

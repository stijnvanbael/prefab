package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Extracts run metrics from a Claude Code {@code --output-format stream-json} transcript.
 *
 * <p>The final {@code result} event carries the authoritative totals. When the run was killed before that event,
 * usage is summed from the assistant messages instead (deduplicated by message id).
 */
final class TranscriptParser {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern MAVEN = Pattern.compile("(^|[\\s;&|(/])(mvn|mvnw)(\\s|$)");

    private TranscriptParser() {
    }

    /**
     * Parses a transcript.
     *
     * @param lines            transcript lines (one JSON event per line)
     * @param forbiddenMarkers strings that must not appear in tool inputs (paths to benchmark sources)
     * @return the metrics
     */
    static Transcript parse(List<String> lines, List<String> forbiddenMarkers) {
        var seenMessages = new HashSet<String>();
        var usage = Agent.TokenUsage.NONE;
        int assistantTurns = 0;
        int buildAttempts = 0;
        boolean contaminated = false;
        var results = new java.util.ArrayList<JsonNode>();
        for (var line : lines) {
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
                            }
                        }
                    }
                }
                case "result" -> results.add(event);
                default -> {
                }
            }
        }
        if (results.isEmpty()) {
            return new Transcript(false, null, usage, assistantTurns, buildAttempts, 0, contaminated, "");
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
                message);
    }

    static Transcript parse(Path transcript, List<String> forbiddenMarkers) {
        try {
            var lines = Files.exists(transcript) ? Files.readAllLines(transcript) : List.<String>of();
            return parse(lines, forbiddenMarkers);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
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
     */
    record Transcript(
            boolean complete,
            Agent.Status status,
            Agent.TokenUsage usage,
            int turns,
            int buildAttempts,
            double costUsd,
            boolean contaminated,
            String finalMessage
    ) {
    }
}

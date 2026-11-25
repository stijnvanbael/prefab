package be.appify.prefab.benchmark;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptParserTest {
    private static final String INIT = """
            {"type":"system","subtype":"init","model":"claude-haiku-5-5"}""";
    private static final String BUILD = """
            {"type":"assistant","message":{"id":"m1","usage":{"input_tokens":10,"output_tokens":5,"cache_read_input_tokens":100,"cache_creation_input_tokens":20},"content":[{"type":"tool_use","name":"Bash","input":{"command":"cd project && mvn -q test"}}]}}""";
    private static final String BUILD_AGAIN = """
            {"type":"assistant","message":{"id":"m1","usage":{"input_tokens":10,"output_tokens":5,"cache_read_input_tokens":100,"cache_creation_input_tokens":20},"content":[{"type":"text","text":"same message, next block"}]}}""";
    private static final String PEEK = """
            {"type":"assistant","message":{"id":"m2","usage":{"input_tokens":1,"output_tokens":2},"content":[{"type":"tool_use","name":"Read","input":{"file_path":"/repo/benchmark/tasks/x/hidden/T.java"}}]}}""";
    private static final String RESULT = """
            {"type":"result","subtype":"success","is_error":false,"num_turns":7,"total_cost_usd":0.12,"result":"Done.","usage":{"input_tokens":50,"output_tokens":60,"cache_read_input_tokens":700,"cache_creation_input_tokens":80}}""";

    @Test
    void usesTheResultEventForTotals() {
        var transcript = TranscriptParser.parse(List.of(INIT, BUILD, BUILD_AGAIN, RESULT), List.of("benchmark/tasks/"));

        assertThat(transcript.complete()).isTrue();
        assertThat(transcript.status()).isEqualTo(Agent.Status.COMPLETED);
        assertThat(transcript.turns()).isEqualTo(7);
        assertThat(transcript.costUsd()).isEqualTo(0.12);
        assertThat(transcript.usage()).isEqualTo(new Agent.TokenUsage(50, 60, 700, 80));
        assertThat(transcript.buildAttempts()).isEqualTo(1);
        assertThat(transcript.contaminated()).isFalse();
        assertThat(transcript.finalMessage()).isEqualTo("Done.");
    }

    @Test
    void sumsAssistantMessagesWhenTheRunWasKilled() {
        var transcript = TranscriptParser.parse(List.of(INIT, BUILD, BUILD_AGAIN, "{truncated", PEEK), List.of());

        assertThat(transcript.complete()).isFalse();
        assertThat(transcript.turns()).isEqualTo(2);
        assertThat(transcript.usage()).isEqualTo(new Agent.TokenUsage(11, 7, 100, 20));
    }

    @Test
    void addsUpFollowUpQueriesAfterTheMainAnswer() {
        var followUp = """
                {"type":"result","subtype":"success","is_error":false,"num_turns":1,"total_cost_usd":0.15,"result":"Stale notification, nothing to do.","usage":{"input_tokens":5,"output_tokens":6,"cache_read_input_tokens":7,"cache_creation_input_tokens":8}}""";

        var transcript = TranscriptParser.parse(List.of(INIT, BUILD, RESULT, followUp), List.of());

        assertThat(transcript.turns()).isEqualTo(8);
        assertThat(transcript.costUsd()).isEqualTo(0.15);
        assertThat(transcript.usage()).isEqualTo(new Agent.TokenUsage(55, 66, 707, 88));
        assertThat(transcript.finalMessage()).isEqualTo("Done.");
    }

    @Test
    void timesTheFirstSuccessfulMavenBuild() {
        var compile = assistantBash("m3", "t1", "mvn -q compile");
        var failed = toolResult("t1", true, "[ERROR] COMPILATION ERROR");
        var listing = assistantBash("m4", "t2", "ls src");
        var listed = toolResult("t2", false, "main test");
        var retry = assistantBash("m5", "t3", "mvn -q test 2>&1 | tail -20");
        var hiddenFailure = toolResult("t3", false, "Tests run: 3, Failures: 1, Errors: 0");
        var again = assistantBash("m6", "t4", "./mvnw -q test");
        var green = toolResult("t4", false, "");
        var lines = List.of(INIT, compile, failed, listing, listed, retry, hiddenFailure, again, green, RESULT);

        var transcript = TranscriptParser.parse(lines, List.of(0L, 1_000L, 5_000L, 6_000L, 6_100L, 7_000L, 9_000L,
                10_000L, 42_000L, 43_000L), List.of());

        assertThat(transcript.firstGreenBuild()).contains(java.time.Duration.ofSeconds(42));
        assertThat(transcript.buildAttempts()).isEqualTo(3);
    }

    @Test
    void firstGreenBuildIsUnknownWithoutMatchingArrivalTimes() {
        var lines = List.of(assistantBash("m3", "t1", "mvn compile"), toolResult("t1", false, "BUILD SUCCESS"));

        assertThat(TranscriptParser.parse(lines, List.of()).firstGreenBuild()).isEmpty();
        assertThat(TranscriptParser.parse(lines, List.of(1L), List.of()).firstGreenBuild()).isEmpty();
        assertThat(TranscriptParser.parse(lines, List.of(1L, 2L), List.of()).firstGreenBuild())
                .contains(java.time.Duration.ofMillis(2));
    }

    private static String assistantBash(String messageId, String toolId, String command) {
        return """
                {"type":"assistant","message":{"id":"%s","usage":{"input_tokens":1,"output_tokens":1},"content":[{"type":"tool_use","id":"%s","name":"Bash","input":{"command":"%s"}}]}}"""
                .formatted(messageId, toolId, command);
    }

    private static String toolResult(String toolId, boolean error, String output) {
        return """
                {"type":"user","message":{"role":"user","content":[{"type":"tool_result","tool_use_id":"%s","is_error":%s,"content":"%s"}]}}"""
                .formatted(toolId, error, output);
    }

    @Test
    void detectsReadsOfBenchmarkSources() {
        assertThat(TranscriptParser.parse(List.of(PEEK), List.of("benchmark/tasks/")).contaminated()).isTrue();
    }

    @Test
    void mapsMaxTurnsAndErrors() {
        var maxTurns = RESULT.replace("\"success\"", "\"error_max_turns\"");
        var error = RESULT.replace("\"is_error\":false", "\"is_error\":true");

        assertThat(TranscriptParser.parse(List.of(maxTurns), List.of()).status()).isEqualTo(Agent.Status.MAX_TURNS);
        assertThat(TranscriptParser.parse(List.of(error), List.of()).status()).isEqualTo(Agent.Status.ERROR);
        var limited = RESULT.replace("\"num_turns\"", "\"api_error_status\":429,\"num_turns\"");
        assertThat(TranscriptParser.parse(List.of(limited), List.of()).status()).isEqualTo(Agent.Status.RATE_LIMITED);
    }
}

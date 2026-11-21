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

package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaudeCodeAgentTest {
    private static final ModelProfile WITH_SETTINGS = new ModelProfile("local", "Local", "qwen", null,
            List.of("--settings", "{}"), null, null);

    @Test
    void refusesAProfileWhoseSettingsWouldReplaceTheSandbox() {
        var sandbox = new Isolation.ClaudeSandbox(List.of(Path.of("/repo")));

        assertThatThrownBy(() -> new ClaudeCodeAgent(WITH_SETTINGS, "claude", List.of(), sandbox))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'local' passes --settings");
    }

    @Test
    void acceptsProfileSettingsWithoutTheSandbox() {
        assertThatCode(() -> new ClaudeCodeAgent(WITH_SETTINGS, "claude", List.of(), Isolation.none()))
                .doesNotThrowAnyException();
    }
}

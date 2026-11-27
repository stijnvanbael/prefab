package be.appify.prefab.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelProfileTest {

    @TempDir
    Path directory;

    @Test
    void loadsEndpointTimeoutAndParallelism() throws IOException {
        Files.writeString(directory.resolve("local.yml"), """
                description: local
                model: qwen3-coder:30b
                env:
                  ANTHROPIC_BASE_URL: http://localhost:11434
                args: ["--effort", "low"]
                timeoutFactor: 3
                parallel: 1
                """);

        var profile = ModelProfile.load(directory, "local");

        assertThat(profile.name()).isEqualTo("local");
        assertThat(profile.model()).isEqualTo("qwen3-coder:30b");
        assertThat(profile.env()).containsEntry("ANTHROPIC_BASE_URL", "http://localhost:11434");
        assertThat(profile.args()).containsExactly("--effort", "low");
        assertThat(profile.timeoutFactor()).isEqualTo(3);
        assertThat(profile.parallel()).isEqualTo(1);
    }

    @Test
    void defaultsMatchAHostedModel() throws IOException {
        Files.writeString(directory.resolve("hosted.yml"), "model: claude-haiku-5-5\n");

        var profile = ModelProfile.load(directory, "hosted");

        assertThat(profile.env()).isEmpty();
        assertThat(profile.args()).isEmpty();
        assertThat(profile.timeoutFactor()).isEqualTo(1);
        assertThat(profile.parallel()).isEqualTo(2);
        assertThat(ModelProfile.hosted("claude-haiku-5-5").parallel()).isEqualTo(2);
    }

    @Test
    void rejectsUnknownProfilesFieldsAndMissingModels() throws IOException {
        Files.writeString(directory.resolve("typo.yml"), "model: x\nenviroment: {}\n");
        Files.writeString(directory.resolve("empty.yml"), "description: no model\n");

        assertThatThrownBy(() -> ModelProfile.load(directory, "absent")).hasMessageContaining("Unknown model profile");
        assertThatThrownBy(() -> ModelProfile.load(directory, "typo")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> ModelProfile.load(directory, "empty")).hasMessageContaining("needs a model");
    }

    @Test
    void modelOptionOverridesTheProfileModelOnly() {
        var profile = new ModelProfile("local", "d", "qwen3-coder:30b", java.util.Map.of("A", "b"), null, 3, 1);

        var other = profile.withModel("qwen3:14b");

        assertThat(other.model()).isEqualTo("qwen3:14b");
        assertThat(other.name()).isEqualTo("local");
        assertThat(other.env()).isEqualTo(profile.env());
        assertThat(other.timeoutFactor()).isEqualTo(3);
    }

    @Test
    void listsTheShippedProfiles() {
        var shipped = Path.of("models");

        assertThat(ModelProfile.available(shipped)).contains("qwen-local", "qwen38-local", "haiku", "sonnet");
        ModelProfile.available(shipped).forEach(name -> assertThat(ModelProfile.load(shipped, name).model()).isNotBlank());
    }
}

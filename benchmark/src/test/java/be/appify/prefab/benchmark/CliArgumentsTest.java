package be.appify.prefab.benchmark;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CliArgumentsTest {

    @Test
    void parsesCommandOptionsFlagsAndPositionals() {
        var arguments = CliArguments.parse("run", "--model", "claude-haiku-5-5", "--tasks=rest-*, avro",
                "--keep-workspaces", "--repeat", "3", "extra");

        assertThat(arguments.command()).isEqualTo("run");
        assertThat(arguments.option("model")).contains("claude-haiku-5-5");
        assertThat(arguments.listOption("tasks")).containsExactly("rest-*", "avro");
        assertThat(arguments.intOption("repeat", 1)).isEqualTo(3);
        assertThat(arguments.intOption("parallel", 2)).isEqualTo(2);
        assertThat(arguments.flag("keep-workspaces")).isTrue();
        assertThat(arguments.positional()).isEqualTo(List.of("extra"));
    }

    @Test
    void defaultsToHelp() {
        assertThat(CliArguments.parse().command()).isEqualTo("help");
    }

    @Test
    void rejectsMissingValuesAndNonPositiveCounts() {
        assertThatThrownBy(() -> CliArguments.parse("run", "--model")).hasMessageContaining("--model");
        assertThatThrownBy(() -> CliArguments.parse("run", "--repeat", "0").intOption("repeat", 1))
                .hasMessageContaining("at least 1");
    }
}

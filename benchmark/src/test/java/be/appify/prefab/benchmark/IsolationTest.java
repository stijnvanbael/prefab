package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IsolationTest {

    @Test
    void wrapsTheCommandInAPrivateMountNamespace() {
        var isolation = new Isolation(List.of(Path.of("/repo"), Path.of("/work")));

        var command = isolation.wrap(List.of("claude", "-p", "hi"), Path.of("/work/run/task/project"), Path.of("/tmp/stage"));

        assertThat(command).startsWith("unshare", "--mount", "--propagation", "private", "--", "sh", "-c");
        assertThat(command.subList(8, command.size()))
                .containsExactly("isolate", "/work/run/task/project", "/tmp/stage", "/repo", "/work", "--",
                        "claude", "-p", "hi");
    }

    @Test
    void leavesTheCommandAloneWhenDisabled() {
        assertThat(Isolation.none().wrap(List.of("claude"), Path.of("/w"), Path.of("/s"))).containsExactly("claude");
        assertThat(Isolation.none().enabled()).isFalse();
    }
}

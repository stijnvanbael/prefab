package be.appify.prefab.benchmark;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkCliTest {
    private static final Path HOME = Path.of(System.getProperty("user.home"));

    @Test
    void hidesTheFolderThatHoldsTheCheckout() {
        assertThat(BenchmarkCli.sourceFolder(HOME.resolve("projects/acme/prefab")))
                .contains(HOME.resolve("projects/acme"));
    }

    @Test
    void neverHidesTheHomeDirectoryOrTheRoot() {
        assertThat(BenchmarkCli.sourceFolder(HOME.resolve("prefab"))).isEmpty();
        assertThat(BenchmarkCli.sourceFolder(Path.of("/prefab"))).isEmpty();
    }
}

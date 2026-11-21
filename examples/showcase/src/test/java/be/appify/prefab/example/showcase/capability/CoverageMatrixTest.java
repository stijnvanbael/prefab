package be.appify.prefab.example.showcase.capability;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageMatrixTest {

    @Test
    @DisplayName("examples/README.md coverage matrix matches the capability files of the variants")
    void readme_matrixMatchesCapabilityFiles() throws IOException {
        var examples = CoverageMatrix.locateExamplesDirectory();
        var matrix = new CoverageMatrix(examples);
        var expected = matrix.render(matrix.variants());

        var actual = CoverageMatrix.embeddedTable(Files.readString(examples.resolve("README.md")));

        assertThat(actual)
                .as("Coverage matrix in examples/README.md is out of date. Replace it with:%n%s", expected)
                .isEqualTo(expected);
    }
}

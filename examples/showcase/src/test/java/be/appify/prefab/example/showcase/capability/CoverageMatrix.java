package be.appify.prefab.example.showcase.capability;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Renders the technology coverage matrix (variant × capability) from the capability files of the showcase variants.
 *
 * <p>{@code examples/README.md} embeds the rendered table between {@link #BEGIN} and {@link #END}; a test compares the
 * two so the documentation cannot drift from the variants.</p>
 */
final class CoverageMatrix {

    static final String BEGIN = "<!-- coverage-matrix:begin -->";
    static final String END = "<!-- coverage-matrix:end -->";

    private static final String REFERENCE_VARIANT = "showcase";
    private static final String SUPPORTED = "✅";
    private static final String UNSUPPORTED = "❌";
    private static final List<String> BACKLOG_FOLDERS = List.of("tasks", "completed", "archive");

    private final Path examplesDirectory;

    CoverageMatrix(Path examplesDirectory) {
        this.examplesDirectory = examplesDirectory;
    }

    /**
     * Finds the {@code examples} directory by walking up from the working directory of the running module.
     *
     * @return the examples directory
     */
    static Path locateExamplesDirectory() {
        for (var directory = Path.of("").toAbsolutePath(); directory != null; directory = directory.getParent()) {
            if (Files.isRegularFile(directory.resolve(REFERENCE_VARIANT).resolve("pom.xml"))) {
                return directory;
            }
        }
        throw new IllegalStateException("Cannot find the examples directory above " + Path.of("").toAbsolutePath());
    }

    /**
     * Reads the capabilities of every variant: the reference showcase first, then the others alphabetically.
     *
     * @return the capabilities of all variants
     */
    List<VariantCapabilities> variants() {
        try (var directories = Files.list(examplesDirectory)) {
            return directories
                    .filter(directory -> isVariant(directory.getFileName().toString()))
                    .filter(directory -> Files.isRegularFile(directory.resolve("pom.xml")))
                    .sorted(Comparator.comparing((Path directory) -> !isReference(directory))
                            .thenComparing(directory -> directory.getFileName().toString()))
                    .map(this::read)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list " + examplesDirectory, e);
        }
    }

    /**
     * Renders the Markdown table for the given variants.
     *
     * @param variants the variants, one column each
     * @return the table, without trailing newline
     */
    String render(List<VariantCapabilities> variants) {
        var header = Stream.concat(Stream.of("Capability"), variants.stream().map(VariantCapabilities::variant));
        var rows = Arrays.stream(Capability.values()).map(capability -> row(capability, variants));
        return Stream.concat(
                        Stream.of(line(header), line(Stream.concat(Stream.of("---"), variants.stream().map(variant -> ":---:")))),
                        rows)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Extracts the table that {@code README.md} embeds between the matrix markers.
     *
     * @param readme the content of the README
     * @return the embedded table
     * @throws IllegalStateException if the markers are missing
     */
    static String embeddedTable(String readme) {
        var begin = readme.indexOf(BEGIN);
        var end = readme.indexOf(END);
        if (begin < 0 || end < begin) {
            throw new IllegalStateException("examples/README.md must contain %s and %s around the coverage matrix"
                    .formatted(BEGIN, END));
        }
        return readme.substring(begin + BEGIN.length(), end).strip().replace("\r\n", "\n");
    }

    private String row(Capability capability, List<VariantCapabilities> variants) {
        return line(Stream.concat(
                Stream.of(capability.name()),
                variants.stream().map(variant -> variant.gap(capability).map(this::unsupportedCell).orElse(SUPPORTED))));
    }

    private String unsupportedCell(CapabilityGap gap) {
        return "%s [%s](%s)".formatted(UNSUPPORTED, gap.task(), taskLink(gap.task()));
    }

    private String taskLink(String task) {
        var prefix = task.toLowerCase() + " - ";
        var file = BACKLOG_FOLDERS.stream()
                .map(folder -> backlogDirectory().resolve(folder))
                .filter(Files::isDirectory)
                .flatMap(CoverageMatrix::listFiles)
                .filter(candidate -> candidate.getFileName().toString().startsWith(prefix))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Gap task %s does not exist in the backlog".formatted(task)));
        var path = "../backlog/%s/%s".formatted(file.getParent().getFileName(), file.getFileName());
        return encode(path).replace("(", "%28").replace(")", "%29");
    }

    private Path backlogDirectory() {
        return examplesDirectory.resolveSibling("backlog");
    }

    private static String encode(String path) {
        try {
            return new URI(null, null, path, null).getRawPath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot link " + path, e);
        }
    }

    private static Stream<Path> listFiles(Path folder) {
        try {
            return Files.list(folder);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list " + folder, e);
        }
    }

    private VariantCapabilities read(Path directory) {
        var file = directory.resolve("src/test/resources").resolve(VariantCapabilities.FILE_NAME);
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Variant %s has no %s".formatted(directory.getFileName(), file));
        }
        try {
            var capabilities = VariantCapabilities.parse(file.toString(), Files.readString(file));
            if (!capabilities.variant().equals(directory.getFileName().toString())) {
                throw new IllegalStateException("%s declares variant '%s' but lives in %s"
                        .formatted(file, capabilities.variant(), directory.getFileName()));
            }
            return capabilities;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    private static boolean isVariant(String directoryName) {
        return directoryName.equals(REFERENCE_VARIANT)
                || (directoryName.startsWith(REFERENCE_VARIANT + "-") && !directoryName.equals("showcase-variant-parent"));
    }

    private static boolean isReference(Path directory) {
        return directory.getFileName().toString().equals(REFERENCE_VARIANT);
    }

    private static String line(Stream<String> cells) {
        return cells.collect(Collectors.joining(" | ", "| ", " |"));
    }
}

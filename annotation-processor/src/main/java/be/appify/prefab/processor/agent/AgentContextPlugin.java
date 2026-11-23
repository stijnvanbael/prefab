package be.appify.prefab.processor.agent;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.tools.StandardLocation;

/**
 * Writes a compact Markdown summary of the Prefab features and generated surface of the project being compiled, so
 * that AI agents do not have to read generic documentation or browse {@code target/generated-sources}.
 *
 * <p>The file is written to {@code target/prefab/agent-context.md} (or {@code target/prefab/agent-context-test.md}
 * when compiling test sources). Disable it with the compiler option
 * {@code -Aprefab.plugin.agentcontext.enabled=false}.
 */
public class AgentContextPlugin implements PrefabPlugin {
    /** Directory below {@code target/} that receives the file. */
    public static final String OUTPUT_DIRECTORY = "prefab";
    /** File name written when compiling main sources. */
    public static final String MAIN_FILE_NAME = "agent-context.md";
    /** File name written when compiling test sources. */
    public static final String TEST_FILE_NAME = "agent-context-test.md";

    private static final String PROBE_RESOURCE = "prefab-agent-context-probe";

    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeGlobalFiles(
            List<ClassManifest> manifests,
            List<PolymorphicAggregateManifest> polymorphicManifests
    ) {
        if (manifests.isEmpty() && polymorphicManifests.isEmpty()) {
            return;
        }
        var markdown = new AgentContextRenderer(context).render(manifests, polymorphicManifests);
        write(markdown);
    }

    private void write(String markdown) {
        var filer = context.processingEnvironment().getFiler();
        try {
            var probe = filer.getResource(StandardLocation.CLASS_OUTPUT, "", PROBE_RESOURCE).toUri();
            if ("file".equals(probe.getScheme())) {
                writeBelowTarget(Paths.get(probe), markdown);
                return;
            }
            try (var writer = filer.createResource(
                    StandardLocation.CLASS_OUTPUT, "", OUTPUT_DIRECTORY + "/" + MAIN_FILE_NAME).openWriter()) {
                writer.write(markdown);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write the Prefab agent context file", e);
        }
    }

    private static void writeBelowTarget(Path probe, String markdown) throws IOException {
        var classesDirectory = probe.getParent();
        var fileName = classesDirectory.getFileName().toString().startsWith("test")
                ? TEST_FILE_NAME : MAIN_FILE_NAME;
        var directory = classesDirectory.getParent().resolve(OUTPUT_DIRECTORY);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(fileName), markdown, StandardCharsets.UTF_8);
    }
}

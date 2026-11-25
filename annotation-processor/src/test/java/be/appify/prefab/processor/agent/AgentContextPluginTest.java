package be.appify.prefab.processor.agent;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class AgentContextPluginTest {
    /** Documented size budget: a fixed part plus a part per aggregate. See the developer guide. */
    private static final int BASE_BUDGET_CHARACTERS = 2_500;
    private static final int PER_AGGREGATE_BUDGET_CHARACTERS = 700;
    private static final String RESOURCE = "prefab/agent-context.md";

    private static final List<JavaFileObject> SOURCES = List.of(
            sourceOf("agentcontext/source/OrderEvent.java"),
            sourceOf("agentcontext/source/Order.java"),
            sourceOf("agentcontext/source/Invoice.java"));

    @Test
    void listsEndpointsPerAggregate() throws IOException {
        var markdown = contextOf(compile());

        assertThat(markdown)
                .contains("### Order (`agentcontext.source`)")
                .contains("`POST /orders` (create)")
                .contains("`GET /orders` (list)")
                .contains("`GET /orders/{id}` (get by id)")
                .contains("`PUT /orders/{id}/rename` (update via rename)")
                .contains("`DELETE /orders/{id}` (delete)");
    }

    @Test
    void listsEventsHandlersAndTopics() throws IOException {
        var markdown = contextOf(compile());

        assertThat(markdown)
                .contains("- `OrderEvent` on topic `orders`")
                .contains("## Generated conformance tests")
                .contains("Opt out per aggregate or event with `conformanceTest = false`")
                .contains("- Handles: `OrderEvent.Placed` via `on()`");
    }

    @Test
    void listsCapabilitiesPresentOnTheClasspathAndOmitsTheOthers() throws IOException {
        var markdown = contextOf(compile());

        assertThat(markdown)
                .contains("**Kafka events**")
                .contains("**MongoDB persistence**")
                .contains("`spring.kafka.bootstrap-servers`")
                .contains("`spring.data.mongodb.uri`")
                .contains("`mvn test-compile spring-boot:test-run`")
                .contains("collection `order`")
                .doesNotContain("Pub/Sub events")
                .doesNotContain("SNS/SQS events")
                .doesNotContain("spring.cloud.gcp.project-id");
    }

    @Test
    void isDeterministic() throws IOException {
        assertThat(contextOf(compile())).isEqualTo(contextOf(compile()));
    }

    @Test
    void staysWithinTheSizeBudget() throws IOException {
        var markdown = contextOf(compile());

        assertThat(markdown.length()).isLessThanOrEqualTo(BASE_BUDGET_CHARACTERS + 2 * PER_AGGREGATE_BUDGET_CHARACTERS);
    }

    @Test
    void canBeDisabledWithAProcessorOption() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .withOptions("-Aprefab.plugin.agentcontext.enabled=false")
                .compile(SOURCES);

        assertThat(compilation).succeeded();
        assertThat(compilation.generatedFile(StandardLocation.CLASS_OUTPUT, RESOURCE)).isEmpty();
    }

    @Test
    void writesBelowTargetWhenOutputIsAFileSystemDirectory(@TempDir Path temp) throws IOException {
        var classes = Files.createDirectories(temp.resolve("target").resolve("classes"));
        var sourceRoot = Files.createDirectories(temp.resolve("src"));
        var files = SOURCES.stream().map(source -> copyTo(sourceRoot, source)).toList();
        var compiler = ToolProvider.getSystemJavaCompiler();

        var result = compiler.run(null, null, null, Stream.concat(
                Stream.of("-d", classes.toString(), "-processor", PrefabProcessor.class.getName()),
                files.stream().map(Path::toString)).toArray(String[]::new));

        assertThat(result).isZero();
        assertThat(temp.resolve("target/prefab/agent-context.md")).exists();
        assertThat(Files.readString(temp.resolve("target/prefab/agent-context.md"), StandardCharsets.UTF_8))
                .contains("### Order");
    }

    private static Path copyTo(Path directory, JavaFileObject source) {
        try {
            var name = Path.of(source.toUri().getPath()).getFileName().toString();
            var target = directory.resolve(name);
            Files.writeString(target, source.getCharContent(true));
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Compilation compile() {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(SOURCES);
        assertThat(compilation).succeeded();
        return compilation;
    }

    private static String contextOf(Compilation compilation) throws IOException {
        var file = compilation.generatedFile(StandardLocation.CLASS_OUTPUT, RESOURCE);
        assertThat(file).isPresent();
        return file.orElseThrow().getCharContent(true).toString();
    }
}

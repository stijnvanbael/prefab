package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * An existing application that brownfield tasks start from, in {@code benchmark/apps/<name>/}: an {@code app.yml} with
 * a description and the Prefab modules it needs, and a {@code project/} directory overlaid on the template before the
 * task's own starter.
 *
 * <p>Base applications are snapshots, not links: a brownfield task must start from the same code in every run, so that
 * runs made months apart stay comparable.
 *
 * @param name        directory name, referenced by {@code base} in a task's {@code task.yml}
 * @param description what the application is and where it came from
 * @param modules     Prefab modules the application depends on
 * @param directory   the application directory
 */
record BaseApp(String name, String description, Set<PrefabModule> modules, Path directory) {
    private static final YAMLMapper YAML = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    BaseApp {
        modules = Set.copyOf(modules);
    }

    /** The files overlaid on the template. */
    Path project() {
        return directory.resolve("project");
    }

    static BaseApp load(Path appsDirectory, String name) {
        var directory = appsDirectory.resolve(name);
        var file = directory.resolve("app.yml");
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("unknown base application '" + name + "': " + file + " does not exist");
        }
        try {
            var spec = YAML.readValue(Files.readString(file), AppSpec.class);
            var app = new BaseApp(name, Objects.requireNonNull(spec.description(), "description is required"),
                    Set.copyOf(spec.modules() == null ? List.of() : spec.modules()), directory);
            if (!Files.isDirectory(app.project())) {
                throw new IllegalArgumentException("base application '" + name + "' has no project/ directory");
            }
            return app;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    record AppSpec(String description, List<PrefabModule> modules) {
    }
}

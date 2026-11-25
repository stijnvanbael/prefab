package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * How the benchmark talks to a model, defined in {@code benchmark/models/<name>.yml}.
 *
 * <p>The agent stays Claude Code; a profile changes which model it drives and where that model is served. That keeps
 * the benchmark independent of one vendor: any model behind an Anthropic-compatible endpoint (a local Ollama server, a
 * gateway) is one YAML file, without code changes. Without a profile, {@code --model <id>} behaves as before.
 *
 * @param name          profile name (file name without extension)
 * @param description   what the profile represents
 * @param model         model id passed to {@code claude --model}
 * @param env           environment variables set for the agent process only (for example the endpoint)
 * @param args          additional CLI arguments for the agent
 * @param timeoutFactor multiplier for the task timeout, for models that are slower than the hosted default
 * @param parallel      default number of simultaneous attempts, overridden by {@code --parallel}
 */
record ModelProfile(String name, String description, String model, Map<String, String> env, List<String> args,
        Integer timeoutFactor, Integer parallel) {

    private static final YAMLMapper YAML = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    ModelProfile {
        env = env == null ? Map.of() : Map.copyOf(env);
        args = args == null ? List.of() : List.copyOf(args);
        timeoutFactor = timeoutFactor == null ? 1 : timeoutFactor;
        parallel = parallel == null ? 2 : parallel;
        if (timeoutFactor < 1 || parallel < 1) {
            throw new IllegalArgumentException("Profile '" + name + "': timeoutFactor and parallel must be at least 1");
        }
    }

    /** The profile of a plain {@code --model <id>}: hosted endpoint, default timeouts and parallelism. */
    static ModelProfile hosted(String model) {
        return new ModelProfile(model, "Hosted model " + model, model, null, null, null, null);
    }

    static ModelProfile load(Path modelsDirectory, String name) {
        var file = modelsDirectory.resolve(name + ".yml");
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Unknown model profile '" + name + "': " + file + " does not exist");
        }
        var spec = YAML.readValue(file.toFile(), ModelProfile.class);
        if (spec.model() == null || spec.model().isBlank()) {
            throw new IllegalArgumentException("Model profile '" + name + "' needs a model");
        }
        return new ModelProfile(name, spec.description(), spec.model(), spec.env(), spec.args(), spec.timeoutFactor(),
                spec.parallel());
    }

    static List<String> available(Path modelsDirectory) {
        if (!Files.isDirectory(modelsDirectory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(modelsDirectory)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(file -> file.endsWith(".yml"))
                    .map(file -> file.substring(0, file.length() - ".yml".length()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list model profiles in " + modelsDirectory, e);
        }
    }

    /** The same profile with another model id, for {@code --model} next to {@code --profile}. */
    ModelProfile withModel(String otherModel) {
        return new ModelProfile(name, description, otherModel, env, args, timeoutFactor, parallel);
    }
}

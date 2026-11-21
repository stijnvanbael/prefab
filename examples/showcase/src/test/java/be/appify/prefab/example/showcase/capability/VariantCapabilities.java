package be.appify.prefab.example.showcase.capability;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * The capability gaps of one showcase variant, read from its {@code showcase-capabilities.yml}.
 *
 * <p>Every capability that is not listed is supported. File format:</p>
 * <pre>{@code
 * variant: showcase-pubsub
 * unsupported:
 *   STREAMS:
 *     reason: no Pub/Sub backend for Prefab Streams
 *     task: TASK-341
 * }</pre>
 *
 * @param variant the variant name, equal to its module directory
 * @param gaps    the unsupported capabilities, in {@link Capability} order
 */
public record VariantCapabilities(String variant, List<CapabilityGap> gaps) {

    /** Name of the capabilities file in the test resources of every variant. */
    public static final String FILE_NAME = "showcase-capabilities.yml";

    private static final Pattern TASK_ID = Pattern.compile("TASK-\\d+(\\.\\d+)*");
    private static final Set<String> ENTRY_KEYS = Set.of("reason", "task");

    /**
     * Creates the capabilities of a variant.
     *
     * @param variant the variant name
     * @param gaps    the unsupported capabilities
     */
    public VariantCapabilities {
        gaps = gaps.stream().sorted(Comparator.comparing(CapabilityGap::capability)).toList();
    }

    /**
     * Reads the capabilities file of the variant that is running, from the test classpath.
     *
     * @return the capabilities of the running variant
     * @throws IllegalStateException if the file is missing or invalid
     */
    public static VariantCapabilities fromClasspath() {
        try (var stream = openClasspathFile()) {
            return parse(FILE_NAME, new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + FILE_NAME, e);
        }
    }

    /**
     * Parses and validates a capabilities file.
     *
     * @param source name of the file, used in error messages
     * @param yaml   the file content
     * @return the parsed capabilities
     * @throws IllegalStateException if the content is invalid
     */
    public static VariantCapabilities parse(String source, String yaml) {
        return parse(source, new StringReader(yaml));
    }

    private static VariantCapabilities parse(String source, Reader reader) {
        var options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        var root = asMap(new Yaml(options).load(reader), source, source);
        requireKeys(root, Set.of("variant", "unsupported"), source);
        var variant = text(root.get("variant"), source + ": 'variant' is required");
        var gaps = asMap(root.get("unsupported"), source, source + ": 'unsupported'").entrySet().stream()
                .map(entry -> parseGap(source, entry.getKey(), entry.getValue()))
                .toList();
        return new VariantCapabilities(variant, gaps);
    }

    /**
     * Returns whether the variant supports a capability.
     *
     * @param capability the capability
     * @return {@code true} if the variant does not list it as unsupported
     */
    public boolean supports(Capability capability) {
        return gap(capability).isEmpty();
    }

    /**
     * Returns the gap for a capability.
     *
     * @param capability the capability
     * @return the gap, or empty if the variant supports the capability
     */
    public Optional<CapabilityGap> gap(Capability capability) {
        return gaps.stream().filter(gap -> gap.capability() == capability).findFirst();
    }

    /**
     * Describes a gap as it appears in skipped tests, e.g.
     * {@code showcase-pubsub: STREAMS not supported — no Pub/Sub backend for Prefab Streams (TASK-341)}.
     *
     * @param gap the gap
     * @return the skip message
     */
    public String skipMessage(CapabilityGap gap) {
        return "%s: %s not supported — %s (%s)".formatted(variant, gap.capability(), gap.reason(), gap.task());
    }

    private static InputStream openClasspathFile() {
        var stream = VariantCapabilities.class.getClassLoader().getResourceAsStream(FILE_NAME);
        if (stream == null) {
            throw new IllegalStateException(
                    "%s not found on the test classpath. Every showcase variant must declare its capability gaps."
                            .formatted(FILE_NAME));
        }
        return stream;
    }

    private static CapabilityGap parseGap(String source, String name, Object value) {
        var capability = parseCapability(source, name);
        var where = "%s: unsupported capability %s".formatted(source, name);
        var entry = asMap(value, source, where);
        requireKeys(entry, ENTRY_KEYS, where);
        var reason = text(entry.get("reason"), where + " needs a 'reason'");
        var task = text(entry.get("task"), where + " needs the backlog 'task' that tracks the gap");
        if (!TASK_ID.matcher(task).matches()) {
            throw new IllegalStateException("%s: task '%s' is not a backlog task ID like TASK-123".formatted(where, task));
        }
        return new CapabilityGap(capability, reason, task);
    }

    private static Capability parseCapability(String source, String name) {
        try {
            return Capability.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "%s: unknown capability '%s', expected one of %s".formatted(source, name, List.of(Capability.values())),
                    e);
        }
    }

    private static void requireKeys(Map<String, Object> map, Set<String> allowed, String where) {
        map.keySet().stream()
                .filter(key -> !allowed.contains(key))
                .findFirst()
                .ifPresent(key -> {
                    throw new IllegalStateException("%s: unexpected key '%s', expected %s".formatted(where, key, allowed));
                });
    }

    private static String text(Object value, String message) {
        if (value instanceof String text && !text.isBlank()) {
            return text.strip();
        }
        throw new IllegalStateException(message);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value, String source, String where) {
        return switch (value) {
            case null -> Map.of();
            case Map<?, ?> map -> (Map<String, Object>) map;
            default -> throw new IllegalStateException("%s: expected a mapping in %s".formatted(where, source));
        };
    }
}

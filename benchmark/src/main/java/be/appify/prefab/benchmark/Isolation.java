package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hides the Prefab repository and other workspaces from the agent, so it cannot read hidden tests, reference solutions
 * or the documentation that its context setup leaves out. Agents do search the file system: without this, Haiku found
 * the repository checkout and read the reference solution, and a local model ran {@code find /} and read another
 * Prefab application next to the checkout.
 *
 * <p>Two mechanisms exist, chosen by {@link #hiding(List)}:
 * <ul>
 *     <li>{@link MountNamespace} on Linux as root: the agent runs in a private mount namespace in which every hidden
 *     path is covered by an empty tmpfs.</li>
 *     <li>{@link ClaudeSandbox} on macOS: Claude Code's own sandbox denies reads of the hidden paths to the agent's
 *     shell commands, and permission rules deny them to its file tools.</li>
 * </ul>
 * Elsewhere isolation is unavailable ({@link None}); the transcript scan for benchmark paths ({@link TranscriptParser})
 * still marks attempts that peeked as contaminated.
 */
sealed interface Isolation {

    static Isolation none() {
        return new None();
    }

    /** Isolation hiding the given paths with the best mechanism of this platform, or {@link #none()}. */
    static Isolation hiding(List<Path> paths) {
        var hidden = paths.stream().map(path -> path.toAbsolutePath().normalize()).distinct().toList();
        if (MountNamespace.supported()) {
            return new MountNamespace(hidden);
        }
        return ClaudeSandbox.supported() ? new ClaudeSandbox(hidden) : none();
    }

    /** Whether the agent is kept from reading the hidden paths. */
    default boolean enabled() {
        return true;
    }

    /**
     * Wraps the agent command so that it runs in the workspace with the hidden paths covered.
     *
     * @param command   the command
     * @param workspace the attempt's workspace, which stays visible
     * @param stage     an empty directory outside the hidden paths, used to carry the workspace across
     * @return the wrapped command, or the command itself when this mechanism does not wrap
     */
    default List<String> wrap(List<String> command, Path workspace, Path stage) {
        return command;
    }

    /**
     * Arguments for the agent CLI that hide the paths from inside the agent.
     *
     * @param workspace the attempt's workspace, which stays visible
     * @param agentTemp the agent's own temp directory, which stays visible
     * @return the arguments, empty when this mechanism needs none
     */
    default List<String> agentArguments(Path workspace, Path agentTemp) {
        return List.of();
    }

    /** No isolation: commands run unchanged. */
    record None() implements Isolation {
        @Override
        public boolean enabled() {
            return false;
        }
    }

    /**
     * A private mount namespace ({@code unshare --mount}) in which every hidden path is covered by an empty tmpfs and
     * only the attempt's own workspace is mounted back. Docker keeps working, because containers are started by the
     * daemon outside the namespace.
     *
     * @param hidden paths to hide
     */
    record MountNamespace(List<Path> hidden) implements Isolation {
        private static final String SCRIPT = """
                set -e
                workspace="$1"; stage="$2"; shift 2
                mount --bind "$workspace" "$stage"
                while [ "$1" != "--" ]; do mount -t tmpfs -o size=1m tmpfs "$1"; shift; done
                shift
                mkdir -p "$workspace"
                mount --bind "$stage" "$workspace"
                cd "$workspace"
                exec "$@"
                """;

        public MountNamespace {
            hidden = List.copyOf(hidden);
        }

        static boolean supported() {
            return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")
                    && "root".equals(System.getProperty("user.name"))
                    && Path.of("/usr/bin/unshare").toFile().canExecute();
        }

        @Override
        public List<String> wrap(List<String> command, Path workspace, Path stage) {
            var wrapped = new ArrayList<>(List.of("unshare", "--mount", "--propagation", "private", "--", "sh", "-c",
                    SCRIPT, "isolate", workspace.toAbsolutePath().normalize().toString(),
                    stage.toAbsolutePath().normalize().toString()));
            hidden.forEach(path -> wrapped.add(path.toString()));
            wrapped.add("--");
            wrapped.addAll(command);
            return List.copyOf(wrapped);
        }
    }

    /**
     * Claude Code's sandbox ({@code --settings}): the agent's shell commands run with reads of the hidden paths denied
     * by the operating system ("Operation not permitted"), and its file tools are refused by permission deny rules.
     *
     * <p>The sandbox can re-allow a path inside a hidden one, so the workspace and the agent's temp directory stay
     * visible even when they lie in a hidden folder. Permission rules cannot; for a hidden folder that contains a
     * visible path, every other entry in it is denied as found when the attempt starts. The parent of the agent's temp
     * directory is hidden too, so attempts cannot read each other's tool output. The Docker socket and local ports stay
     * reachable, so the agent's tests can use Testcontainers.
     *
     * @param hidden paths to hide
     */
    record ClaudeSandbox(List<Path> hidden) implements Isolation {
        private static final JsonMapper JSON = JsonMapper.builder().build();
        private static final List<String> DOCKER_SOCKETS = List.of(
                Path.of(System.getProperty("user.home"), ".docker/run/docker.sock").toString(),
                "/var/run/docker.sock");
        /** macOS links these to {@code /private/...}; rules match the spelling the agent uses, so both are denied. */
        private static final List<String> LINKED_ROOTS = List.of("/var/", "/tmp/", "/etc/");

        public ClaudeSandbox {
            hidden = List.copyOf(hidden);
        }

        static boolean supported() {
            return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
        }

        @Override
        public List<String> agentArguments(Path workspace, Path agentTemp) {
            return List.of("--settings", JSON.writeValueAsString(settings(workspace, agentTemp)));
        }

        Map<String, Object> settings(Path workspace, Path agentTemp) {
            var visible = List.of(realPath(workspace), realPath(agentTemp));
            var hiddenPaths = new LinkedHashSet<Path>();
            hidden.stream().map(ClaudeSandbox::realPath).forEach(hiddenPaths::add);
            hiddenPaths.add(realPath(agentTemp).getParent());
            var filesystem = Map.of(
                    "denyRead", hiddenPaths.stream().map(Path::toString).toList(),
                    "allowRead", visible.stream().map(Path::toString).toList());
            var sandbox = new LinkedHashMap<String, Object>();
            sandbox.put("enabled", true);
            sandbox.put("autoAllowBashIfSandboxed", true);
            sandbox.put("allowUnsandboxedCommands", false);
            sandbox.put("network", Map.of("allowUnixSockets", DOCKER_SOCKETS, "allowLocalBinding", true));
            sandbox.put("filesystem", filesystem);
            var denied = hiddenPaths.stream().flatMap(path -> deniedEntries(path, visible).stream()).toList();
            return Map.of("sandbox", sandbox, "permissions", Map.of("deny", denyRules(denied)));
        }

        /** The entries of a hidden path to deny so that the visible paths in it stay readable. */
        private static List<Path> deniedEntries(Path hiddenPath, List<Path> visible) {
            if (visible.stream().anyMatch(hiddenPath::startsWith)) {
                return List.of();
            }
            if (visible.stream().noneMatch(path -> path.startsWith(hiddenPath))) {
                return List.of(hiddenPath);
            }
            try (Stream<Path> entries = Files.list(hiddenPath)) {
                return entries.flatMap(entry -> deniedEntries(entry, visible).stream()).toList();
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot list " + hiddenPath + " to hide its contents", e);
            }
        }

        private static List<String> denyRules(List<Path> denied) {
            return denied.stream()
                    .flatMap(path -> spellings(path).stream())
                    .flatMap(path -> Stream.of("Read(/" + path + "/**)", "Edit(/" + path + "/**)",
                            "Read(/" + path + ")", "Edit(/" + path + ")"))
                    .toList();
        }

        private static List<String> spellings(Path path) {
            var text = path.toString();
            return LINKED_ROOTS.stream()
                    .filter(root -> text.startsWith("/private" + root))
                    .findFirst()
                    .map(root -> List.of(text, text.substring("/private".length())))
                    .orElse(List.of(text));
        }

        private static Path realPath(Path path) {
            var absolute = path.toAbsolutePath().normalize();
            try {
                return Files.exists(absolute) ? absolute.toRealPath() : absolute;
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot resolve " + absolute, e);
            }
        }
    }
}

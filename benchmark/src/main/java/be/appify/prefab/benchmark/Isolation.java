package be.appify.prefab.benchmark;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hides the Prefab repository and other workspaces from the agent, so it cannot read hidden tests, reference solutions
 * or the documentation that its context setup leaves out. Agents do search the file system: without this, Haiku found
 * the repository checkout and read the reference solution.
 *
 * <p>On Linux as root, the agent runs in a private mount namespace ({@code unshare --mount}) in which every hidden path
 * is covered by an empty tmpfs and only the attempt's own workspace is mounted back. Docker keeps working, because
 * containers are started by the daemon outside the namespace. Elsewhere isolation is unavailable; the transcript scan
 * for benchmark paths ({@link TranscriptParser}) still marks attempts that peeked as contaminated.
 *
 * @param hidden paths to hide; when empty, commands run unchanged
 */
record Isolation(List<Path> hidden) {
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

    Isolation {
        hidden = List.copyOf(hidden);
    }

    static Isolation none() {
        return new Isolation(List.of());
    }

    /** Isolation hiding the given paths when the platform supports it, otherwise {@link #none()}. */
    static Isolation hiding(List<Path> paths) {
        return supported() ? new Isolation(paths.stream().map(path -> path.toAbsolutePath().normalize()).toList())
                : none();
    }

    static boolean supported() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")
                && "root".equals(System.getProperty("user.name"))
                && Path.of("/usr/bin/unshare").toFile().canExecute();
    }

    boolean enabled() {
        return !hidden.isEmpty();
    }

    /**
     * Wraps a command so that it runs in the workspace with the hidden paths covered.
     *
     * @param command   the command
     * @param workspace the attempt's workspace, which stays visible
     * @param stage     an empty directory outside the hidden paths, used to carry the workspace across
     * @return the wrapped command, or the command itself when isolation is disabled
     */
    List<String> wrap(List<String> command, Path workspace, Path stage) {
        if (!enabled()) {
            return command;
        }
        var wrapped = new ArrayList<>(List.of("unshare", "--mount", "--propagation", "private", "--", "sh", "-c",
                SCRIPT, "isolate", workspace.toAbsolutePath().normalize().toString(),
                stage.toAbsolutePath().normalize().toString()));
        hidden.forEach(path -> wrapped.add(path.toString()));
        wrapped.add("--");
        wrapped.addAll(command);
        return List.copyOf(wrapped);
    }
}

package be.appify.prefab.benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minimal git wrapper used to snapshot a workspace, so the harness can tell what the agent changed and can reset the
 * workspace between acceptance checks.
 */
final class Git {
    private static final Duration TIMEOUT = Duration.ofMinutes(2);
    private static final Map<String, String> IDENTITY = Map.of(
            "GIT_AUTHOR_NAME", "prefab-benchmark",
            "GIT_AUTHOR_EMAIL", "benchmark@prefab.invalid",
            "GIT_COMMITTER_NAME", "prefab-benchmark",
            "GIT_COMMITTER_EMAIL", "benchmark@prefab.invalid");

    private final Path directory;
    private final Path log;

    Git(Path directory, Path log) {
        this.directory = directory;
        this.log = log;
    }

    void init() {
        git("init", "--quiet", "--initial-branch=main");
    }

    /** Stages everything (respecting .gitignore) and commits it, tagging the commit. */
    void commitAll(String tag) {
        git("add", "--all");
        git("commit", "--quiet", "--allow-empty", "--no-verify", "-m", tag);
        git("tag", "--force", tag);
    }

    /** Restores tracked and untracked (non-ignored) files to the given tag. */
    void resetTo(String tag) {
        git("reset", "--quiet", "--hard", tag);
        git("clean", "--quiet", "-fd");
    }

    /** Lists files that differ between a tag and the working tree, with a status letter (A, M, D). */
    List<ChangedFile> changesSince(String tag) {
        git("add", "--all");
        var output = capture("diff", "--cached", "--name-status", "--no-renames", tag);
        var changes = new ArrayList<ChangedFile>();
        for (var line : output.lines().toList()) {
            var parts = line.split("\t", 2);
            if (parts.length == 2) {
                changes.add(new ChangedFile(parts[0].trim().charAt(0), parts[1].trim()));
            }
        }
        return List.copyOf(changes);
    }

    /** The full diff between a tag and the working tree. */
    String diffSince(String tag) {
        git("add", "--all");
        return capture("diff", "--cached", "--no-renames", tag);
    }

    /** Short id of the checked-out commit, suffixed with {@code -dirty} when the working tree has changes. */
    String describeHead() {
        var commit = capture("rev-parse", "--short", "HEAD").strip();
        var dirty = !capture("status", "--porcelain").isBlank();
        return dirty ? commit + "-dirty" : commit;
    }

    private String capture(String... args) {
        try {
            var output = Files.createTempFile("git-", ".out");
            try {
                run(output, args);
                return Files.readString(output);
            } finally {
                Files.deleteIfExists(output);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void git(String... args) {
        run(log, args);
    }

    private void run(Path output, String... args) {
        var command = new ArrayList<String>();
        command.add("git");
        command.addAll(List.of(args));
        var options = new ProcessRunner.ProcessOptions(directory, IDENTITY, List.of(), output, log, TIMEOUT);
        var result = ProcessRunner.run(command, options);
        if (!result.succeeded()) {
            throw new IllegalStateException("git " + String.join(" ", args) + " failed in " + directory + ", see " + log);
        }
    }

    /**
     * A file changed relative to a snapshot.
     *
     * @param status git status letter: A (added), M (modified) or D (deleted)
     * @param path   path relative to the workspace root, with forward slashes
     */
    record ChangedFile(char status, String path) {
    }
}

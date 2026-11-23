package be.appify.prefab.processor.dbmigration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;

/**
 * Locates the Flyway {@code db/migration} directory that is visible to the annotation processor, so that generated and
 * hand-written migrations can be read.
 */
public final class MigrationDirectory {
    private static final int MAX_VERSION_PROBE = 100;
    private static final List<String> CLASSPATH_PROBE_SUFFIXES = List.of("generated", "manual", "init", "baseline");
    private static final Pattern VERSIONED_FILE = Pattern.compile("^V(\\d+)__.*\\.sql$");

    private MigrationDirectory() {
    }

    /**
     * Finds the migration directory on the compile classpath or in the compiler output.
     *
     * @param processingEnvironment
     *         the processing environment
     * @return the directory, or empty when the project has no migrations yet
     * @throws IOException
     *         when the file system cannot be queried
     */
    public static Optional<Path> locate(ProcessingEnvironment processingEnvironment) throws IOException {
        var fromClassPath = fromClassPath(processingEnvironment);
        if (fromClassPath != null) {
            return Optional.of(fromClassPath);
        }
        return Optional.ofNullable(fromClassOutput(processingEnvironment));
    }

    /**
     * Extracts the Flyway version of a versioned migration file name.
     *
     * @param filename
     *         the file name, for example {@code V3__add_status.sql}
     * @return the version, or {@code -1} when the name is not a versioned migration
     */
    public static int versionOf(String filename) {
        var matcher = VERSIONED_FILE.matcher(filename);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1;
    }

        private static Path fromClassPath(ProcessingEnvironment processingEnvironment)
            throws IOException {
        for (int i = 1; i <= MAX_VERSION_PROBE; i++) {
            var dir = fromClassPathVersion(processingEnvironment, i);
            if (dir != null) {
                return dir;
            }
        }
        return fromClasspathEntries();
    }

    private static Path fromClassPathVersion(
            ProcessingEnvironment processingEnvironment, int version) throws IOException {
        for (String suffix : CLASSPATH_PROBE_SUFFIXES) {
            try {
                var file = processingEnvironment.getFiler().getResource(
                        StandardLocation.CLASS_PATH, "db.migration",
                        "V%d__%s.sql".formatted(version, suffix));
                return Path.of(file.toUri()).getParent();
            } catch (FileNotFoundException | FileSystemNotFoundException ignored) {
            }
        }
        return null;
    }

    private static Path fromClasspathEntries() {
        var classPath = System.getProperty("java.class.path");
        if (classPath == null || classPath.isBlank()) {
            return null;
        }
        for (String classPathEntry : classPath.split(java.io.File.pathSeparator)) {
            try {
                var candidate = Path.of(classPathEntry).resolve("db").resolve("migration");
                if (java.nio.file.Files.isDirectory(candidate)) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Path fromClassOutput(ProcessingEnvironment processingEnvironment)
            throws IOException {
        try {
            var file = processingEnvironment.getFiler().getResource(
                    StandardLocation.CLASS_OUTPUT, "", "db/migration/.probe");
            var dir = Path.of(file.toUri()).getParent();
            return java.nio.file.Files.isDirectory(dir) ? dir : null;
        } catch (FileNotFoundException | FileSystemNotFoundException ignored) {
            return null;
        }
    }
}

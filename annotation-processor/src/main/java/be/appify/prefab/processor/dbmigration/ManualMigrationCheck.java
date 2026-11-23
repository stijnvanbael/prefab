package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.error.PrefabProcessingException;
import be.appify.prefab.processor.handwritten.HandWrittenCodePlugin;
import be.appify.prefab.processor.handwritten.HandWrittenFindings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

/**
 * Reports hand-written Flyway migrations that change a table Prefab manages for an aggregate.
 *
 * <p>Generated migrations ({@code V<n>__generated.sql}), baselines, files named {@code ...manual.sql} and files that
 * contain the comment {@code -- prefab:manual} are the documented escape hatches and are not reported. Statements that
 * only change data ({@code INSERT}, {@code UPDATE}, {@code DELETE}) never trigger the check.
 */
public final class ManualMigrationCheck {
    /** Comment that marks a migration as intentionally hand-written. */
    public static final String MANUAL_MARKER = "-- prefab:manual";

    private static final Pattern EXEMPT_FILE_NAME = Pattern.compile("(?i)^V\\d+__(.*[_-])?(generated|manual|baseline)\\.sql$");
    private static final Pattern MARKER = Pattern.compile("(?im)^\\s*--\\s*prefab:\\s*manual\\b");
    private static final Pattern COMMENTS = Pattern.compile("--[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);
    private static final String TABLE = "((?:\"?\\w+\"?\\.)?\"?\\w+\"?)";
    private static final Pattern TABLE_STATEMENT = Pattern.compile(
            "(?is)\\b(CREATE\\s+TABLE|ALTER\\s+TABLE|DROP\\s+TABLE)(?:\\s+IF\\s+(?:NOT\\s+)?EXISTS)?(?:\\s+ONLY)?\\s+" + TABLE);
    private static final Pattern INDEX_STATEMENT = Pattern.compile(
            "(?is)\\b(CREATE)\\s+(?:UNIQUE\\s+)?INDEX\\b[^;]*?\\bON\\s+(?:ONLY\\s+)?" + TABLE);

    private final PrefabContext context;

    ManualMigrationCheck(PrefabContext context) {
        this.context = context;
    }

    /**
     * Checks the hand-written migrations of the project.
     *
     * @param managedTables
     *         the tables Prefab derives from the aggregates
     * @param aggregateNames
     *         the simple names of the aggregates, used to name the aggregate that owns a table
     */
    void check(Set<String> managedTables, Collection<String> aggregateNames) {
        if (!context.configuration().isPluginEnabled(HandWrittenCodePlugin.class) || managedTables.isEmpty()) {
            return;
        }
        var findings = HandWrittenFindings.of(context, HandWrittenFindings.Severity.WARNING);
        handWrittenMigrations().forEach(migration -> {
            var touched = touchedManagedTables(migration, managedTables);
            if (!touched.isEmpty()) {
                findings.report(null, describe(migration, touched, aggregateNames), suggestedFix(migration));
            }
        });
    }

    private List<Path> handWrittenMigrations() {
        try {
            var directory = MigrationDirectory.locate(context.processingEnvironment());
            if (directory.isEmpty()) {
                return List.of();
            }
            try (var files = Files.list(directory.get())) {
                return files
                        .filter(Files::isRegularFile)
                        .filter(path -> MigrationDirectory.versionOf(path.getFileName().toString()) >= 0)
                        .filter(path -> !EXEMPT_FILE_NAME.matcher(path.getFileName().toString()).matches())
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
            }
        } catch (IOException e) {
            throw new PrefabProcessingException(null, "Prefab",
                    "cannot list the hand-written database migrations in db/migration: %s".formatted(e.getMessage()),
                    "check that the db/migration directory is readable, or disable the check with"
                            + " -Aprefab.plugin.handwrittencode.enabled=false",
                    e);
        }
    }

    private static Map<String, String> touchedManagedTables(Path migration, Set<String> managedTables) {
        var sql = read(migration);
        if (MARKER.matcher(sql).find()) {
            return Map.of();
        }
        var withoutComments = COMMENTS.matcher(sql).replaceAll(" ");
        var touched = new LinkedHashMap<String, String>();
        for (var pattern : List.of(TABLE_STATEMENT, INDEX_STATEMENT)) {
            var matcher = pattern.matcher(withoutComments);
            while (matcher.find()) {
                var table = tableName(matcher.group(2));
                if (managedTables.contains(table)) {
                    touched.putIfAbsent(table, matcher.group(1).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT));
                }
            }
        }
        return touched;
    }

    private static String tableName(String reference) {
        var unqualified = reference.substring(reference.lastIndexOf('.') + 1);
        return unqualified.replace("\"", "").toLowerCase(Locale.ROOT);
    }

    private static String read(Path migration) {
        try {
            return Files.readString(migration);
        } catch (IOException e) {
            throw new PrefabProcessingException(null, "Prefab",
                    "cannot read database migration %s: %s".formatted(migration.getFileName(), e.getMessage()),
                    "check that the file is readable and valid UTF-8", e);
        }
    }

    private static String describe(Path migration, Map<String, String> touched, Collection<String> aggregateNames) {
        var changes = touched.entrySet().stream()
                .map(entry -> "%s `%s` (aggregate %s)".formatted(
                        entry.getValue(), entry.getKey(), aggregateOf(entry.getKey(), aggregateNames)))
                .toList();
        return "db/migration/%s is a hand-written migration that changes tables Prefab already manages: %s. Prefab derives"
                .formatted(migration.getFileName(), String.join(", ", changes))
                + " these tables from the aggregates and generates the migrations for them";
    }

    private static String aggregateOf(String table, Collection<String> aggregateNames) {
        return aggregateNames.stream()
                .filter(name -> table.equals(toSnakeCase(name)) || table.startsWith(toSnakeCase(name) + "_"))
                .max(Comparator.comparingInt(String::length))
                .orElse("unknown");
    }

    private static String suggestedFix(Path migration) {
        return ("change the @Aggregate model instead and compile: Prefab writes the matching V<n>__generated.sql. If"
                + " you copy a generated script into src/main/resources/db/migration, keep the `__generated` suffix."
                + " If this DDL really cannot be expressed in the model, mark %s as intentionally hand-written by"
                + " naming it V<n>__<description>_manual.sql or by adding the comment `%s` to it")
                .formatted(migration.getFileName(), MANUAL_MARKER);
    }
}

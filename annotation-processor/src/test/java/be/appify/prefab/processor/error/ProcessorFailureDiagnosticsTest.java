package be.appify.prefab.processor.error;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Negative compilation tests for the failure categories described in {@code docs}: expected input failures,
 * unexpected plugin failures, resource failures and fatal errors.
 */
class ProcessorFailureDiagnosticsTest {

    private record InvalidMigration(String name, String baseline, String migration, List<String> expectedFragments) {
    }

    private static JavaFileObject aggregate(String name) {
        return JavaFileObjects.forSourceString(
                "test.failure." + name, """
                        package test.failure;

                        import be.appify.prefab.core.annotations.Aggregate;
                        import be.appify.prefab.core.annotations.rest.Create;
                        import be.appify.prefab.core.service.Reference;
                        import org.springframework.data.annotation.Id;
                        import org.springframework.data.annotation.Version;

                        @Aggregate
                        public record %s(@Id Reference<%s> id, @Version long version, String name) {
                            @Create
                            public %s(String name) {
                                this(Reference.create(), 0L, name);
                            }
                        }
                        """.formatted(name, name, name));
    }

    private static Compilation compileWith(String failureMode, String... extraOptions) {
        var options = new java.util.ArrayList<String>();
        options.add("-A" + FailureInjectingTestPlugin.OPTION + "=" + failureMode);
        options.addAll(List.of(extraOptions));
        return javac().withProcessors(new PrefabProcessor()).withOptions(options)
                .compile(aggregate("Alpha"), aggregate("Beta"));
    }

    private static List<String> errors(Compilation compilation) {
        return compilation.errors().stream().map(d -> d.getMessage(null)).toList();
    }

    private static void assertContains(String message, String... fragments) {
        for (var fragment : fragments) {
            assertTrue(message.contains(fragment), () -> "Expected <" + fragment + "> in:\n" + message);
        }
    }

    private static void assertDoesNotContain(String message, String... fragments) {
        for (var fragment : fragments) {
            assertFalse(message.contains(fragment), () -> "Unexpected <" + fragment + "> in:\n" + message);
        }
    }

    @Test
    @DisplayName("an expected failure from a plugin is one actionable error per aggregate with element and fix")
    void expectedPluginFailure_reportsOneActionableErrorPerIndependentAggregate() {
        var compilation = compileWith("expected");

        assertThat(compilation).failed();
        assertEquals(2, compilation.errors().size(), () -> errors(compilation).toString());
        compilation.errors().forEach(error -> {
            assertContains(
                    error.getMessage(null), "[@Aggregate]", "injected invalid input",
                    "Suggested fix: remove the injected failure",
                    "Caused by: java.lang.IllegalStateException: root cause");
            assertDoesNotContain(error.getMessage(null), "Internal error");
            assertNotNull(error.getSource());
        });
        assertEquals(2, compilation.errors().stream().map(d -> d.getSource().getName()).distinct().count());
    }

    @Test
    @DisplayName("an unexpected plugin failure names the plugin, keeps the cause chain and hints at the stack trace")
    void unexpectedPluginFailure_namesPluginAndCause() {
        var compilation = compileWith("unexpected");

        assertThat(compilation).failed();
        assertEquals(2, compilation.errors().size());
        var message = errors(compilation).getFirst();
        assertContains(
                message,
                "Internal error in plugin " + FailureInjectingTestPlugin.class.getName(),
                "java.lang.IllegalStateException: injected bug",
                "Caused by: java.lang.NullPointerException: inner cause",
                "-Aprefab.diagnostics.stacktrace=true");
        assertDoesNotContain(message, "\tat ");
        assertTrue(
                errors(compilation).stream().allMatch(error -> error.contains("Internal error")),
                () -> "no consequent compiler errors expected, but got " + errors(compilation));
    }

    @Test
    @DisplayName("-Aprefab.diagnostics.stacktrace=true adds the full stack trace of an unexpected failure")
    void unexpectedPluginFailure_withStackTraceOption_includesStackTrace() {
        var compilation = compileWith("unexpected", "-A" + ErrorReporter.STACKTRACE_OPTION + "=true");

        assertThat(compilation).failed();
        assertContains(
                errors(compilation).getFirst(),
                "\tat " + FailureInjectingTestPlugin.class.getName() + ".writeService",
                "Caused by: java.lang.NullPointerException: inner cause");
    }

    @Test
    @DisplayName("fatal JVM errors are never caught and swallowed")
    void fatalError_isNotCaught() {
        var thrown = assertThrows(Throwable.class, () -> compileWith("fatal"));

        var cause = thrown;
        while (cause.getCause() != null && !(cause instanceof AssertionError)) {
            cause = cause.getCause();
        }
        assertTrue(cause instanceof AssertionError, () -> "Expected an AssertionError but got " + thrown);
        assertEquals("injected fatal error", cause.getMessage());
    }

    @Test
    @DisplayName("an unsupported type yields an actionable error instead of an uncaught exception")
    void unsupportedType_reportsActionableError() {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString(
                        "test.failure.Generic", """
                                package test.failure;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.service.Reference;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate
                                public record Generic<T extends Number & Comparable<T>>(
                                        @Id Reference<Generic<T>> id, @Version long version, T value) {}
                                """));

        assertThat(compilation).failed();
        assertTrue(
                errors(compilation).stream().anyMatch(message -> message.contains("cannot map type")
                        && message.contains("Suggested fix") && !message.contains("Internal error")),
                () -> "Unexpected diagnostics: " + errors(compilation));
    }

    @Test
    @DisplayName("an invalid migration transition yields an error naming the file and a corrective action")
    void invalidMigrationTransition_reportsFileAndFix() throws IOException {
        var classpath = Files.createTempDirectory("prefab-failure-migration");
        var migrations = Files.createDirectories(classpath.resolve(Path.of("db", "migration")));
        Files.writeString(migrations.resolve("V1__baseline.sql"), "CREATE TABLE chapter (id VARCHAR(255));");
        Files.writeString(migrations.resolve("V2__broken.sql"), "ALTER TABLE chapter DROP COLUMN missing;");

        var compilation = javac().withProcessors(new PrefabProcessor())
                .withOptions(classpathOptionsWith(classpath))
                .compile(aggregate("Alpha"));

        assertThat(compilation).failed();
        assertTrue(
                errors(compilation).stream().anyMatch(message ->
                        message.contains("V2__broken.sql") && message.contains("cannot replay database migration")
                                && message.contains("ALTER TABLE chapter DROP COLUMN missing")
                                && message.contains("Cannot drop non-existing column: missing")
                                && message.contains("Suggested fix")
                                && message.contains("earlier versioned migrations")
                                && message.contains("ordered as intended")
                                && message.contains("Caused by:")
                                && !message.contains("cannot interpret database migration")
                                && !message.contains("Internal error")),
                () -> "Unexpected diagnostics: " + errors(compilation));
    }

    @Test
    @DisplayName("a duplicate migration table diagnostic names the migration and offending statement")
    void duplicateMigrationTable_reportsFileStatementObjectAndReason() throws IOException {
        var classpath = Files.createTempDirectory("prefab-failure-migration-duplicate-table");
        var migrations = Files.createDirectories(classpath.resolve(Path.of("db", "migration")));
        Files.writeString(
                migrations.resolve("V1__baseline.sql"),
                "CREATE TABLE chapter (id VARCHAR(255));");
        Files.writeString(
                migrations.resolve("V2__duplicate.sql"),
                "CREATE TABLE chapter (replacement BIGINT);");

        var compilation = javac().withProcessors(new PrefabProcessor())
                .withOptions(classpathOptionsWith(classpath))
                .compile(aggregate("Alpha"));

        assertThat(compilation).failed();
        assertTrue(
                errors(compilation).stream().anyMatch(message ->
                        message.contains("V2__duplicate.sql")
                                && message.contains("CREATE TABLE chapter (replacement BIGINT)")
                                && message.contains("table chapter")
                                && message.contains("already exists")
                                && !message.contains("Internal error")),
                () -> "Unexpected diagnostics: " + errors(compilation));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidMigrationTransitions")
    void invalidMigrationTransitions_reportFileStatementObjectAndReason(InvalidMigration scenario) throws IOException {
        var classpath = Files.createTempDirectory("prefab-failure-migration-transition");
        var migrations = Files.createDirectories(classpath.resolve(Path.of("db", "migration")));
        Files.writeString(migrations.resolve("V1__baseline.sql"), scenario.baseline());
        Files.writeString(migrations.resolve("V2__broken.sql"), scenario.migration());

        var compilation = javac().withProcessors(new PrefabProcessor())
                .withOptions(classpathOptionsWith(classpath))
                .compile(aggregate("Alpha"));

        assertThat(compilation).failed();
        var message = errors(compilation).stream()
                .filter(error -> error.contains("V2__broken.sql"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No migration diagnostic found: " + errors(compilation)));
        assertContains(message, "V2__broken.sql", "statement '");
        scenario.expectedFragments().forEach(fragment -> assertContains(message, fragment));
        assertDoesNotContain(message, "Internal error");
    }

    private static Stream<Arguments> invalidMigrationTransitions() {
        var empty = "";
        var chapter = "CREATE TABLE chapter (id VARCHAR(255));";
        var courseAndChapter = """
                CREATE TABLE course (id VARCHAR(255) PRIMARY KEY);
                CREATE TABLE chapter (id VARCHAR(255), course_id VARCHAR(255));
                """;
        return Stream.of(
                new InvalidMigration(
                        "duplicate column", empty,
                        "CREATE TABLE chapter (id VARCHAR(255), id BIGINT);",
                        List.of("CREATE TABLE chapter", "column id", "defined more than once")),
                new InvalidMigration(
                        "duplicate index",
                        chapter + "\nCREATE INDEX chapter_id_ix ON chapter (id);",
                        "CREATE INDEX chapter_id_ix ON chapter (id);",
                        List.of("CREATE INDEX chapter_id_ix", "index chapter_id_ix", "already exists")),
                new InvalidMigration(
                        "duplicate named constraint", courseAndChapter,
                        """
                                ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                                    FOREIGN KEY (course_id) REFERENCES course (id);
                                ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                                    FOREIGN KEY (course_id) REFERENCES course (id);
                                """,
                        List.of(
                                "ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk", "constraint chapter_course_fk",
                                "already in use")),
                new InvalidMigration(
                        "table rename to occupied name",
                        chapter + "\nCREATE TABLE course (id VARCHAR(255));",
                        "ALTER TABLE chapter RENAME TO course;",
                        List.of("ALTER TABLE chapter RENAME TO course", "table chapter", "table with that name")),
                new InvalidMigration(
                        "column rename to occupied name",
                        "CREATE TABLE chapter (id VARCHAR(255), title VARCHAR(255));",
                        "ALTER TABLE chapter RENAME COLUMN id TO title;",
                        List.of("ALTER TABLE chapter RENAME COLUMN id TO title", "column id", "column with that name")),
                new InvalidMigration(
                        "drop missing table", empty, "DROP TABLE missing;",
                        List.of("DROP TABLE missing", "Cannot drop missing table missing")),
                new InvalidMigration(
                        "drop missing index", empty, "DROP INDEX missing_ix;",
                        List.of("DROP INDEX missing_ix", "Cannot drop missing index missing_ix")),
                new InvalidMigration(
                        "drop missing named constraint",
                        "CREATE TABLE chapter (id VARCHAR(255), CONSTRAINT chapter_id_unique UNIQUE (id));",
                        "ALTER TABLE chapter DROP CONSTRAINT missing_constraint;",
                        List.of(
                                "ALTER TABLE chapter DROP CONSTRAINT missing_constraint",
                                "Cannot drop missing constraint missing_constraint")),
                new InvalidMigration(
                        "index references missing table", empty,
                        "CREATE INDEX chapter_id_ix ON chapter (id);",
                        List.of("CREATE INDEX chapter_id_ix", "index chapter_id_ix", "missing table chapter")),
                new InvalidMigration(
                        "index references missing column", chapter,
                        "CREATE INDEX chapter_missing_ix ON chapter (missing);",
                        List.of("CREATE INDEX chapter_missing_ix", "Index chapter_missing_ix", "missing column missing")),
                new InvalidMigration(
                        "foreign key references missing table", chapter,
                        """
                                ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                                    FOREIGN KEY (id) REFERENCES missing_course (id);
                                """,
                        List.of("ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk", "missing table missing_course")),
                new InvalidMigration(
                        "foreign key on added column references missing table", chapter,
                        "ALTER TABLE chapter ADD COLUMN course_id VARCHAR(255) REFERENCES missing_course (id);",
                        List.of("ALTER TABLE chapter ADD COLUMN", "Foreign key on table chapter",
                                "missing table missing_course")),
                new InvalidMigration(
                        "foreign key references missing local column", chapter,
                        """
                                ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                                    FOREIGN KEY (missing) REFERENCES chapter (id);
                                """,
                        List.of("ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk", "missing column missing")),
                new InvalidMigration(
                        "foreign key references missing referenced column", courseAndChapter,
                        """
                                ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk
                                    FOREIGN KEY (course_id) REFERENCES course (missing);
                                """,
                        List.of(
                                "ALTER TABLE chapter ADD CONSTRAINT chapter_course_fk",
                                "referenced side of foreign key", "missing column missing"))
        ).map(Arguments::of);
    }

    @Test
    @DisplayName("guarded schema no-ops and same-migration references compile successfully")
    void guardedNoOpsAndSameMigrationReferences_compileSuccessfully() throws IOException {
        var classpath = Files.createTempDirectory("prefab-valid-migration-transitions");
        var migrations = Files.createDirectories(classpath.resolve(Path.of("db", "migration")));
        Files.writeString(
                migrations.resolve("V1__baseline.sql"),
                "CREATE TABLE chapter (id VARCHAR(255) PRIMARY KEY);");
        Files.writeString(
                migrations.resolve("V2__guarded.sql"), """
                        CREATE TABLE IF NOT EXISTS chapter (replacement BIGINT);
                        ALTER TABLE chapter ADD COLUMN IF NOT EXISTS id BIGINT;
                        CREATE INDEX chapter_id_ix ON chapter (id);
                        CREATE INDEX IF NOT EXISTS chapter_id_ix ON chapter (replacement);
                        CREATE TABLE course (id VARCHAR(255) PRIMARY KEY);
                        CREATE TABLE section (id VARCHAR(255), course_id VARCHAR(255));
                        ALTER TABLE section ADD CONSTRAINT section_course_fk
                            FOREIGN KEY (course_id) REFERENCES course (id);
                        ALTER TABLE section ADD COLUMN other_course_id VARCHAR(255) REFERENCES course (id);
                        DROP TABLE IF EXISTS no_such_table;
                        DROP INDEX IF EXISTS absent_ix;
                        ALTER TABLE chapter DROP CONSTRAINT IF EXISTS absent_constraint;
                        INSERT INTO chapter (id) VALUES ('seed');
                        """);

        var compilation = javac().withProcessors(new PrefabProcessor())
                .withOptions(classpathOptionsWith(classpath))
                .compile(aggregate("Alpha"));

        assertThat(compilation).succeeded();
    }
}

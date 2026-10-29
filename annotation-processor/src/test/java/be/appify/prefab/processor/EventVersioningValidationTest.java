package be.appify.prefab.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventVersioningValidationTest {
    @TempDir
    Path baselineDir;

    private static final String BASELINE = """
            version=1.0.0
            field.name=java.lang.String;required
            """;

    @BeforeEach
    void commitBaseline() throws IOException {
        Files.writeString(baselineDir.resolve("test.versioning.UserCreated.properties"), BASELINE);
    }

    private Compilation compile(String version, String compatibility, String components, String extra) {
        var source = """
                package test.versioning;

                import be.appify.prefab.core.annotations.*;
                import be.appify.prefab.core.versioning.Migration;
                import java.util.Optional;

                @Event(topic = "users", platform = Event.Platform.KAFKA, version = "%s", compatibility = Compatibility.%s)
                public record UserCreated(%s) {
                }
                %s
                """.formatted(version, compatibility, components, extra);
        return javac().withProcessors(new PrefabProcessor())
                .withOptions("-Aprefab.events.baselineDir=" + baselineDir)
                .compile(JavaFileObjects.forSourceString("test.versioning.UserCreated", source));
    }

    private static final String MIGRATION_1_TO_2 = """
            record UserCreatedV1(String name) {}

            @EventMigration(from = "1", to = "2")
            class Migrate implements Migration<UserCreatedV1, UserCreated> {
                public UserCreated migrate(UserCreatedV1 old) {
                    return new UserCreated(old.name(), "x");
                }
            }
            """;

    @Test
    void acceptsAnUnchangedContract() {
        assertThat(compile("1.0.0", "BACKWARD", "String name", "")).succeeded();
    }

    @Test
    void acceptsAPatchBumpWithoutChanges() {
        assertThat(compile("1.0.1", "BACKWARD", "String name", "")).succeeded();
    }

    @Test
    void acceptsABackwardCompatibleMinorChange() {
        assertThat(compile("1.1.0", "BACKWARD", "String name, @Default(\"x\") String country", "")).succeeded();
    }

    @Test
    void rejectsAChangeWithoutVersionBump() {
        assertThat(compile("1.0.0", "BACKWARD", "String name, Optional<String> country", ""))
                .hadErrorContaining("bump at least the minor version");
    }

    @Test
    void rejectsABreakingChangeInAMinorBump() {
        assertThat(compile("1.1.0", "BACKWARD", "String name, String country", ""))
                .hadErrorContaining("requires a major version bump");
    }

    @Test
    void rejectsRemovingARequiredFieldWhenForwardCompatible() {
        assertThat(compile("1.1.0", "FORWARD", "String other", "")).hadErrorContaining("required field 'name' was removed");
    }

    @Test
    void fullCompatibilityChecksBothDirections() {
        assertThat(compile("1.1.0", "FULL", "String name, String country", ""))
                .hadErrorContaining("required field 'country' was added");
    }

    @Test
    void doesNotCheckCompatibilityWhenNone() {
        assertThat(compile("1.1.0", "NONE", "String other", "")).succeeded();
    }

    @Test
    void rejectsDecreasingVersions() {
        Compilation compilation = compile("0.9.0", "NONE", "String name", "");
        assertThat(compilation).hadErrorContaining("versions must not decrease");
    }

    @Test
    void rejectsAMajorBumpWithoutMigration() {
        assertThat(compile("2.0.0", "BACKWARD", "String name, String country", ""))
                .hadErrorContaining("requires a @EventMigration chain from major version 1 to 2");
    }

    @Test
    void acceptsAMajorBumpWithMigration() {
        assertThat(compile("2.0.0", "BACKWARD", "String name, String country", MIGRATION_1_TO_2)).succeeded();
    }

    @Test
    void rejectsMigrationsThatDoNotEndAtTheCurrentMajor() {
        assertThat(compile("3.0.0", "NONE", "String name, String country", MIGRATION_1_TO_2))
                .hadErrorContaining("end at major version 2");
    }

    @Test
    void rejectsNonContiguousChains() {
        var chain = MIGRATION_1_TO_2.replace("UserCreatedV1, UserCreated", "UserCreatedV1, UserCreatedV3")
                .replace("Migrate", "First").replace("return new UserCreated(old.name(), \"x\");",
                        "return new UserCreatedV3(old.name());") + """
                record UserCreatedV3(String name) {}

                @EventMigration(from = "3", to = "4")
                class Second implements Migration<UserCreatedV3, UserCreated> {
                    public UserCreated migrate(UserCreatedV3 old) {
                        return new UserCreated(old.name(), "x");
                    }
                }
                """;
        assertThat(compile("4.0.0", "NONE", "String name, String country", chain))
                .hadErrorContaining("not contiguous");
    }

    @Test
    void renamedFieldsWithPreviousNameAreNotRemovals() {
        assertThat(compile("1.1.0", "BACKWARD", "@PreviousName(\"name\") String fullName", "")).succeeded();
    }

    @Test
    void writesTheBaselineOfTypesWithoutOne() throws IOException {
        Files.delete(baselineDir.resolve("test.versioning.UserCreated.properties"));

        assertThat(compile("1.0.0", "BACKWARD", "String name", "")).succeeded();

        var written = Files.readString(baselineDir.resolve("test.versioning.UserCreated.properties"));
        assertTrue(written.contains("version=1.0.0") && written.contains("field.name=java.lang.String;required"));
    }

    @Test
    void rejectsMinimumSupportedVersionAboveTheVersion() {
        var source = """
                package test.versioning;

                import be.appify.prefab.core.annotations.Event;

                @Event(topic = "users", platform = Event.Platform.KAFKA, version = "1.0.0", minSupportedVersion = "2.0.0")
                public record Other(String name) {
                }
                """;
        var compilation = javac().withProcessors(new PrefabProcessor())
                .withOptions("-Aprefab.events.baselineDir=" + baselineDir)
                .compile(JavaFileObjects.forSourceString("test.versioning.Other", source));

        assertThat(compilation).hadErrorContaining("minSupportedVersion");
    }
}

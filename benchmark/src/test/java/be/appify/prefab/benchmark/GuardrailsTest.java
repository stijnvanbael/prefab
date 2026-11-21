package be.appify.prefab.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class GuardrailsTest {
    @TempDir
    Path workspace;

    private Git.ChangedFile write(String path, String content) throws Exception {
        var file = workspace.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return new Git.ChangedFile('A', path);
    }

    @Test
    void flagsPlainSpringCode() throws Exception {
        var changes = List.of(
                write("src/main/java/com/example/BookController.java", "@RestController\nclass BookController {}"),
                write("src/main/java/com/example/BookEntity.java", "import jakarta.persistence.Entity;\n@Entity class BookEntity {}"),
                write("src/main/java/com/example/Books.java", "interface Books extends JpaRepository<Book, String> {}"),
                write("src/main/java/com/example/Dao.java", "class Dao { JdbcTemplate jdbc; }"),
                write("src/main/java/com/example/Pojo.java", "import lombok.Data;\n@Data class Pojo {}"));

        var violations = Guardrails.check(workspace, changes, Set.of());

        assertThat(violations).extracting(Guardrails.Violation::rule).containsExactlyInAnyOrder(
                "hand-written-controller", "jpa", "spring-data-repository", "hand-written-persistence", "lombok");
        assertThat(violations).allMatch(violation -> violation.severity() == Guardrails.Severity.FAIL);
    }

    @Test
    void flagsOverridesOfGeneratedClasses() throws Exception {
        write("src/main/java/com/example/Book.java", "@Aggregate\npublic record Book(String id) {}");
        var change = write("src/main/java/com/example/application/BookService.java", "class BookService {}");

        assertThat(Guardrails.check(workspace, List.of(change), Set.of()))
                .extracting(Guardrails.Violation::rule).containsExactly("generated-class-override");
    }

    @Test
    void handWrittenMigrationsAreOnlyAWarning() throws Exception {
        var change = write("src/main/resources/db/migration/V2__add.sql", "ALTER TABLE book ADD COLUMN x INT;");

        assertThat(Guardrails.check(workspace, List.of(change), Set.of()))
                .singleElement()
                .satisfies(violation -> assertThat(violation.severity()).isEqualTo(Guardrails.Severity.WARN));
    }

    @Test
    void ignoresIdiomaticPrefabCodeTestsAndAllowedRules() throws Exception {
        var aggregate = write("src/main/java/com/example/Book.java",
                "@Aggregate\n@GetList\npublic record Book(@Id Reference<Book> id) {}");
        var test = write("src/test/java/com/example/BookTest.java", "@RestController class Fake {}");
        var controller = write("src/main/java/com/example/Custom.java", "@RestController class Custom {}");

        assertThat(Guardrails.check(workspace, List.of(aggregate, test), Set.of())).isEmpty();
        assertThat(Guardrails.check(workspace, List.of(controller), Set.of("hand-written-controller"))).isEmpty();
    }
}

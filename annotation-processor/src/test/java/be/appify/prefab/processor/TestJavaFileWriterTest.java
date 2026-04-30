package be.appify.prefab.processor;

import com.palantir.javapoet.TypeSpec;
import java.io.File;
import java.nio.file.Files;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestJavaFileWriterTest {

    @TempDir
    File tempDir;

    @Test
    void overwritesExistingFileWithUpdatedContent() throws Exception {
        var outputPath = new File(tempDir, "target/prefab-test-sources/com/example");
        outputPath.mkdirs();
        var existingFile = new File(outputPath, "MyClass.java");
        Files.writeString(existingFile.toPath(), "// stale content");

        var writer = new TestJavaFileWriterFixture(tempDir.getAbsolutePath());
        writer.writeFile("com.example", "MyClass", TypeSpec.classBuilder("MyClass")
                .addModifiers(Modifier.PUBLIC)
                .build());

        assertTrue(existingFile.exists(), "File should still be present after regeneration");
        assertFalse(Files.readString(existingFile.toPath()).contains("// stale content"),
                "Stale content should have been replaced by regenerated content");
    }

    @Test
    void writesFileWhenItDoesNotExist() throws Exception {
        var outputPath = new File(tempDir, "target/prefab-test-sources/com/example");
        outputPath.mkdirs();
        var expectedFile = new File(outputPath, "NewClass.java");
        assertFalse(expectedFile.exists(), "File should not exist before generation");

        var writer = new TestJavaFileWriterFixture(tempDir.getAbsolutePath());
        writer.writeFile("com.example", "NewClass", TypeSpec.classBuilder("NewClass")
                .addModifiers(Modifier.PUBLIC)
                .build());

        assertTrue(expectedFile.exists(), "Generated file should have been created");
    }

    /**
     * Test-scoped subclass that overrides path resolution so tests do not rely on the real file system layout
     * (i.e., src/main/java paths from real source compilation).
     */
    private static class TestJavaFileWriterFixture extends TestJavaFileWriter {
        private final String fixedRootPath;

        TestJavaFileWriterFixture(String rootPath) {
            super(null, null);
            this.fixedRootPath = rootPath;
        }

        @Override
        public java.util.Optional<String> getRootPath() {
            return java.util.Optional.of(fixedRootPath);
        }
    }
}

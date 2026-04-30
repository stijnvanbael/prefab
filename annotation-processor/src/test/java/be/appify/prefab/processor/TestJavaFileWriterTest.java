package be.appify.prefab.processor;

import com.palantir.javapoet.TypeSpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
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
    void skipsWritingWhenFileAlreadyExists() throws Exception {
        var outputPath = new File(tempDir, "target/prefab-test-sources/com/example");
        outputPath.mkdirs();
        var existingFile = new File(outputPath, "MyClass.java");
        Files.writeString(existingFile.toPath(), "// existing content");

        var captured = new ByteArrayOutputStream();
        var originalOut = System.out;
        System.setOut(new PrintStream(captured));
        try {
            var writer = new TestJavaFileWriterFixture(tempDir.getAbsolutePath());
            writer.writeFile("com.example", "MyClass", TypeSpec.classBuilder("MyClass")
                    .addModifiers(Modifier.PUBLIC)
                    .build());
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(captured.toString().contains("MyClass"), "Expected skip message to mention MyClass");
        assertTrue(captured.toString().contains("already exists"), "Expected skip message to mention 'already exists'");
        assertTrue(existingFile.exists(), "Existing file should still be present");
        assertTrue(Files.readString(existingFile.toPath()).contains("// existing content"),
                "Existing file contents should be unchanged");
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

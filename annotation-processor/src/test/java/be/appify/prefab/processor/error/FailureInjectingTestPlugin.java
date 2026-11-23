package be.appify.prefab.processor.error;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;

/**
 * Test-only plugin that fails on demand. It is inert unless the compilation passes
 * {@code -Aprefab.test.failure=expected|unexpected|error}.
 */
public class FailureInjectingTestPlugin implements PrefabPlugin {
    static final String OPTION = "prefab.test.failure";
    private String mode = "";

    @Override
    public void initContext(PrefabContext context) {
        mode = context.processingEnvironment().getOptions().getOrDefault(OPTION, "");
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        switch (mode) {
            case "expected" -> throw new PrefabProcessingException(manifest.type().asElement(), "@Aggregate",
                    "injected invalid input", "remove the injected failure", new IllegalStateException("root cause"));
            case "unexpected" -> throw new IllegalStateException("injected bug",
                    new NullPointerException("inner cause"));
            case "fatal" -> throw new AssertionError("injected fatal error");
            default -> {
            }
        }
    }
}

package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.FileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabProcessor;
import be.appify.prefab.processor.TestClientWriter;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class SecurityGenerationTest {

    @Test
    void securitySettingsGenerateExpectedControllerAndTestClientSecurity() throws Exception {
        var processor = new CapturingSecurityProcessor();
        var compilation = javac()
                .withProcessors(processor)
                .compile(sourceOf("rest/security/source/Document.java"));

        assertThat(compilation).succeeded();

        var controllerSource = compilation.generatedSourceFile("rest.security.infrastructure.http.DocumentController")
                .orElseThrow()
                .getCharContent(false)
                .toString();
        Assertions.assertTrue(controllerSource.contains("@PreAuthorize(\"hasRole('READER')\")"));
        Assertions.assertTrue(controllerSource.contains("@PreAuthorize(\"permitAll()\")"));
        Assertions.assertTrue(controllerSource.contains("@PreAuthorize(\"hasAuthority('ROLE_EDITOR')\")"));
        Assertions.assertTrue(controllerSource.contains("@PreAuthorize(\"hasRole('EDITOR')\")"));
        Assertions.assertTrue(controllerSource.contains("@PreAuthorize(\"hasRole('SEARCHER')\")"));
        Assertions.assertTrue(controllerSource.contains("@PreAuthorize(\"hasAuthority('ROLE_ATTACHMENT_READ')\")"));
        Assertions.assertTrue(controllerSource.contains("@PreAuthorize(\"hasAuthority('ROLE_STREAM')\")"));

        var clientSource = processor.capturedSources.stream()
                .filter(source -> source.contains("class DocumentClient"))
                .findFirst()
                .orElse("");
        Assertions.assertTrue(clientSource.contains(".roles(\"READER\")"));
        Assertions.assertTrue(clientSource.contains(".authorities(new SimpleGrantedAuthority(\"ROLE_EDITOR\"))"));
        Assertions.assertTrue(clientSource.contains(".roles(\"SEARCHER\")"));
        Assertions.assertTrue(clientSource.contains(".authorities(new SimpleGrantedAuthority(\"ROLE_ATTACHMENT_READ\"))"));
        Assertions.assertTrue(clientSource.contains("public DocumentClient as(RequestPostProcessor... requestPostProcessors)"));
        Assertions.assertTrue(clientSource.contains("private RequestPostProcessor applySecurityOverride(RequestPostProcessor defaultPostProcessor)"));
        Assertions.assertTrue(clientSource.contains(".with(applySecurityOverride(SecurityMockMvcRequestPostProcessors.user(\"test\").roles(\"READER\")))"));
        Assertions.assertTrue(clientSource.contains("@Autowired"));
    }

    @Test
    void securitySettingsRejectMixedAuthorityAndRole() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/security/invalidmixed/source/InvalidDocument.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Security supports either authority or role, but not both");
    }

    @SupportedAnnotationTypes({"be.appify.prefab.core.annotations.*"})
    static class CapturingSecurityProcessor extends PrefabProcessor {
        final List<String> capturedSources = new ArrayList<>();

        @Override
        protected TestClientWriter createTestClientWriter(PrefabContext context) {
            return new TestClientWriter(context, new CapturingFileOutput(capturedSources));
        }
    }

    static class CapturingFileOutput implements FileOutput {
        private final List<String> capturedSources;

        CapturingFileOutput(List<String> capturedSources) {
            this.capturedSources = capturedSources;
        }

        @Override
        public void setPreferredElement(TypeElement element) {
        }

        @Override
        public void writeFile(String packagePrefix, String typeName, TypeSpec type) {
            capturedSources.add(JavaFile.builder(packagePrefix, type).build().toString());
        }
    }
}

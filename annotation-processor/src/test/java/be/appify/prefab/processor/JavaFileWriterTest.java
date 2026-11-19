package be.appify.prefab.processor;

import com.palantir.javapoet.TypeSpec;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.JavaFileObjects.forSourceString;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaFileWriterTest {

    @Test
    void noteEmittedWhenGenerationSkippedDueToExistingSourceFile() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("rest/override/source/Widget.java"),
                        sourceOf("rest/override/source/application/WidgetService.java")
                );

        assertThat(compilation).hadNoteContaining("rest.override.application.WidgetService");
        assertThat(compilation).hadNoteContaining("already exists");
    }

    @Test
    void noNoteEmittedWhenFileAlreadyGeneratedByProcessor() {
        var compilation = javac()
                .withProcessors(new DoubleWriteProcessor())
                .compile(sourceOf("rest/override/source/Widget.java"));

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).hadNoteCount(0);
    }

    @Test
    void generatedSourceDeclaresItsModelAsOriginatingElement() {
        var processor = new OriginTrackingPrefabProcessor();
        var compilation = javac()
                .withProcessors(processor)
                .compile(sourceOf("rest/override/source/Widget.java"));

        assertThat(compilation).succeeded();
        assertEquals(
                Set.of("rest.override.Widget"),
                processor.originatingTypesBySource.get("rest.override.infrastructure.http.WidgetController"));
    }

    @Test
    void sharedGeneratedSourceDeclaresAllContributingModelsAsOriginatingElements() {
        var processor = new OriginTrackingPrefabProcessor();
        var compilation = javac()
                .withProcessors(processor)
                .compile(
                        sourceOf("mongodb/indexed/source/Product.java"),
                        forSourceString("mongodb.indexed.Category", """
                                package mongodb.indexed;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.Indexed;
                                import be.appify.prefab.core.service.Reference;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate
                                public record Category(
                                        @Id Reference<Category> id,
                                        @Version long version,
                                        @Indexed String title
                                ) {
                                }
                                """)
                );

        assertThat(compilation).succeeded();
        assertEquals(
                Set.of("mongodb.indexed.Product", "mongodb.indexed.Category"),
                processor.originatingTypesBySource.get(
                        "mongodb.indexed.infrastructure.mongodb.MongoIndexConfiguration"));
    }

    /**
     * A processor that intentionally writes the same file twice to simulate the case
     * where the annotation processor tries to regenerate a file it already produced.
     */
    @SupportedAnnotationTypes("be.appify.prefab.core.annotations.Aggregate")
    static class DoubleWriteProcessor extends AbstractProcessor {

        private boolean alreadyWritten = false;

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!alreadyWritten) {
                alreadyWritten = true;
                var writer = new JavaFileWriter(processingEnv, "application");
                var type = TypeSpec.classBuilder("GeneratedService").build();
                writer.writeFile("rest.override", "GeneratedService", type);
                writer.writeFile("rest.override", "GeneratedService", type);
            }
            return false;
        }
    }

    @SupportedAnnotationTypes("be.appify.prefab.core.annotations.*")
    static class OriginTrackingPrefabProcessor extends PrefabProcessor {
        private final Map<String, Set<String>> originatingTypesBySource = new HashMap<>();

        @Override
        public synchronized void init(ProcessingEnvironment processingEnvironment) {
            var filer = processingEnvironment.getFiler();
            var trackingFiler = (Filer) Proxy.newProxyInstance(
                    Filer.class.getClassLoader(),
                    new Class<?>[]{Filer.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("createSourceFile") && arguments.length == 2) {
                            var originatingElements = (Element[]) arguments[1];
                            var originatingTypes = Arrays.stream(originatingElements)
                                    .map(TypeElement.class::cast)
                                    .map(type -> type.getQualifiedName().toString())
                                    .collect(Collectors.toSet());
                            originatingTypesBySource.put(arguments[0].toString(), originatingTypes);
                        }
                        try {
                            return method.invoke(filer, arguments);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
            var trackingEnvironment = (ProcessingEnvironment) Proxy.newProxyInstance(
                    ProcessingEnvironment.class.getClassLoader(),
                    new Class<?>[]{ProcessingEnvironment.class},
                    (proxy, method, arguments) -> method.getName().equals("getFiler")
                            ? trackingFiler
                            : invoke(method, processingEnvironment, arguments));
            super.init(trackingEnvironment);
        }

        private static Object invoke(
                Method method,
                ProcessingEnvironment processingEnvironment,
                Object[] arguments
        ) throws Throwable {
            try {
                return method.invoke(processingEnvironment, arguments);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}

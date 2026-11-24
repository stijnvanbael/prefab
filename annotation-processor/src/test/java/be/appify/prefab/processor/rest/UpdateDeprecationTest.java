package be.appify.prefab.processor.rest;

import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateDeprecationTest {
    private static final String WARNING = "void updates are deprecated";

    @Test
    void mutableUpdateWarnsOnceOnTheMethodAndKeepsGeneratedBehavior() {
        var source = sourceOf("rest/computed/source/Order.java");
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining(WARNING).inFile(source).onLine(36);
        assertThat(updateWarnings(compilation)).hasSize(1);
        assertThat(compilation).hadWarningContaining("next major version");
        assertThat(compilation).hadWarningContaining("Return an aggregate instance instead");
        assertThat(compilation).generatedSourceFile("rest.computed.application.OrderService")
                .contentsAsUtf8String().contains("aggregate.addLine(");
    }

    @Test
    void immutableUpdateIsNotDeprecatedOrWarned() {
        var compilation = javac().withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/createorupdate/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(updateWarnings(compilation)).isEmpty();
        assertThat(Update.class.isAnnotationPresent(Deprecated.class)).isFalse();
        assertThat(compilation).generatedSourceFile("rest.createorupdate.application.ProductService")
                .contentsAsUtf8String().contains("aggregate = aggregate.update(");
    }

    @Test
    void pairedCreateOrUpdateWarnsAndStillInvokesVoidMethod() {
        var source = contentsOf("rest/createorupdate/source/Product.java")
                .replace("public Product update(String name, String price)",
                        "public void update(String name, String price)")
                .replace("return new Product(id, version, name, price);", "");
        var compilation = javac().withProcessors(new PrefabProcessor())
                .compile(JavaFileObjects.forSourceString("rest.createorupdate.Product", source));

        assertThat(compilation).succeeded();
        assertThat(updateWarnings(compilation)).hasSize(1);
        assertThat(compilation).generatedSourceFile("rest.createorupdate.application.ProductService")
                .contentsAsUtf8String().contains("aggregate.update(");
    }

    @Test
    void asyncVoidWarnsAndReturningCurrentAggregatePreservesAsyncService() {
        var source = contentsOf("rest/asyncupdate/source/Order.java");
        var legacy = javac().withProcessors(new PrefabProcessor())
                .compile(JavaFileObjects.forSourceString("rest.asyncupdate.Order", source));
        var migratedSource = source.replace("public void ship(@NotNull String trackingCode) {",
                        "public Order ship(@NotNull String trackingCode) {")
                .replace("publish(new OrderShipped(id, trackingCode));",
                        "publish(new OrderShipped(id, trackingCode));\n        return this;");
        var migrated = javac().withProcessors(new PrefabProcessor())
                .compile(JavaFileObjects.forSourceString("rest.asyncupdate.Order", migratedSource));

        assertThat(legacy).succeeded();
        assertThat(updateWarnings(legacy)).hasSize(1);
        assertThat(migrated).succeeded();
        assertThat(updateWarnings(migrated)).isEmpty();
        assertThat(migrated).generatedSourceFile("rest.asyncupdate.application.OrderService")
                .contentsAsUtf8String().contains("aggregate = aggregate.ship(");
        assertThat(migrated).generatedSourceFile("rest.asyncupdate.application.OrderService")
                .contentsAsUtf8String().doesNotContain("orderRepository.save(");
        assertThat(migrated).generatedSourceFile("rest.asyncupdate.infrastructure.http.OrderController")
                .contentsAsUtf8String().contains("accepted()");
    }

    @Test
    void polymorphicUpdatesAreWarnedIndependently() {
        var source = contentsOf("rest/polymorphicwithparent/source/Drawing.java")
                .replace("public Circle resize(Reference<Canvas> canvas, double radius)",
                        "public void resize(Reference<Canvas> canvas, double radius)")
                .replace("return new Circle(id, version, canvas, radius);", "")
                .replace("public Rectangle resize(Reference<Canvas> canvas, double width, double height)",
                        "public void resize(Reference<Canvas> canvas, double width, double height)")
                .replace("return new Rectangle(id, version, canvas, width, height);", "");
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                sourceOf("rest/polymorphicwithparent/source/Canvas.java"),
                JavaFileObjects.forSourceString("rest.polymorphicwithparent.Drawing", source));

        assertThat(compilation).succeeded();
        assertThat(updateWarnings(compilation)).hasSize(2);
        assertThat(compilation).generatedSourceFile("rest.polymorphicwithparent.application.DrawingService")
                .contentsAsUtf8String().contains("aggregate.resize(");
    }

    @Test
    void updateAnnotationAloneTriggersProcessor() {
        var source = JavaFileObjects.forSourceString("test.Example", """
                package test;
                import be.appify.prefab.core.annotations.rest.Update;
                class Example {
                    @Update void change() {}
                }
                """);
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining(WARNING).inFile(source).onLine(4);
        assertThat(updateWarnings(compilation)).hasSize(1);
    }

    private static List<Diagnostic<? extends JavaFileObject>> updateWarnings(Compilation compilation) {
        return compilation.warnings().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.WARNING)
                .filter(diagnostic -> diagnostic.getMessage(Locale.ROOT).contains(WARNING))
                .toList();
    }
}

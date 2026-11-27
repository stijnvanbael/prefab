package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class TypeLevelCreateTest {

    private static JavaFileObject book(String annotations, String constructorAnnotation) {
        return JavaFileObjects.forSourceString("test.typelevel.Book", """
                package test.typelevel;

                import be.appify.prefab.core.annotations.Aggregate;
                import be.appify.prefab.core.annotations.AsyncCommit;
                import be.appify.prefab.core.annotations.rest.Create;
                import be.appify.prefab.core.annotations.rest.GetById;
                import be.appify.prefab.core.service.Reference;
                import org.springframework.data.annotation.Id;
                import org.springframework.data.annotation.Version;

                @Aggregate
                %s
                @GetById
                public record Book(@Id Reference<Book> id, @Version long version, String title) {
                    %s
                    public Book(String title) {
                        this(Reference.create(), 0L, title);
                    }
                }
                """.formatted(annotations, constructorAnnotation));
    }

    @Test
    void createOnAggregateTypeFailsCompilationWithConstructorAdvice() {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(book("@Create", ""));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("[@Create] Book: @Create cannot be placed on an @Aggregate type");
        assertThat(compilation).hadErrorContaining("move @Create to the constructor of Book");
    }

    @Test
    void createOnAsyncCommitAggregateTypeSuggestsStaticFactoryMethod() {
        var compilation = javac().withProcessors(new PrefabProcessor())
                .compile(book("@AsyncCommit\n@Create", ""));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("move @Create to a public static factory method of Book");
    }

    @Test
    void createOnConstructorStillCompiles() {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(book("", "@Create"));

        assertThat(compilation).succeeded();
    }

    @Test
    void agentContextOnlyListsGeneratedCreateEndpoints() throws IOException {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(book("", ""));

        assertThat(compilation).succeeded();
        var context = compilation.generatedFile(StandardLocation.CLASS_OUTPUT, "prefab/agent-context.md")
                .orElseThrow().getCharContent(true).toString();
        assertThat(context).contains("`GET /books/{id}` (get by id)").doesNotContain("(create)");
    }
}

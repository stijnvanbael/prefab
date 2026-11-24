package be.appify.prefab.processor.rest;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class ChildAggregateScopeTest {

    private static final Compilation compilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("rest/childscope/source/Project.java"),
                    sourceOf("rest/childscope/source/Task.java"),
                    sourceOf("rest/childscope/source/Plan.java"),
                    sourceOf("rest/childscope/source/Step.java"));

    @Test
    void service_givenChild_checksParentMembership() {
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.childscope.application.TaskService")
                .contentsAsUtf8String()
                .contains("public boolean belongsToParent(String parentId, String id)");
    }

    @Test
    void controller_givenChild_guardsGetByIdUpdateAndDelete() {
        assertThat(compilation).succeeded();
        var controller = assertThat(compilation)
                .generatedSourceFile("rest.childscope.infrastructure.http.TaskController")
                .contentsAsUtf8String();
        controller.contains("String projectId) {\n"
                + "        if (!service.belongsToParent(projectId, id)) {\n"
                + "            return ResponseEntity.notFound().build();");
        controller.contains("ResponseEntity<TaskResponse> getById(");
        controller.contains("ResponseEntity<Void> delete(");
        controller.contains("ResponseEntity<TaskResponse> rename(");
    }

    @Test
    void controller_givenChild_createLocationPointsToNestedPath() {
        assertThat(compilation)
                .generatedSourceFile("rest.childscope.infrastructure.http.TaskController")
                .contentsAsUtf8String()
                .contains("URI.create(\"/projects/\" + projectId + \"/tasks/\" + id)");
    }

    @Test
    void controller_givenParentNamedWithIdSuffix_doesNotDuplicateSuffix() {
        assertThat(compilation).succeeded();
        var controller = assertThat(compilation)
                .generatedSourceFile("rest.childscope.infrastructure.http.StepController")
                .contentsAsUtf8String();
        controller.contains("\"plans/{planId}/steps\"");
        controller.doesNotContain("planIdId");
    }
}

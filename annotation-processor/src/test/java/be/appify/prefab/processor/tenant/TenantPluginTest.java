package be.appify.prefab.processor.tenant;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class TenantPluginTest {

    @Test
    void tenantIdFieldNotInGeneratedCreateRequestRecord() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/project/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("tenant.project.application.CreateProjectRequest")
                .contentsAsUtf8String()
                .doesNotContain("organisationId");
    }

    @Test
    void createServicePopulatesTenantIdFromProvider() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/project/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("tenant.project.application.ProjectService")
                .contentsAsUtf8String()
                .contains("tenantContextProvider.currentTenantId()");
    }

    @Test
    void createServiceReconstructsAggregateWithTenantId() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/project/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("tenant.project.application.ProjectService")
                .contentsAsUtf8String()
                .contains("aggregate = new Project(");
    }

    @Test
    void getByIdServiceFiltersByTenantId() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/project/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("tenant.project.application.ProjectService")
                .contentsAsUtf8String()
                .contains("aggregate.organisationId().equals(tenantContextProvider.currentTenantId())");
    }

    @Test
    void getListServiceFiltersByTenantId() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/project/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("tenant.project.application.ProjectService")
                .contentsAsUtf8String()
                .contains("findByOrganisationId");
    }

    @Test
    void updateServiceFiltersByTenantId() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/project/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("tenant.project.application.ProjectService")
                .contentsAsUtf8String()
                .contains("aggregate.organisationId().equals(tenantContextProvider.currentTenantId())");
    }

    @Test
    void deleteServiceFiltersByTenantId() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/project/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("tenant.project.application.ProjectService")
                .contentsAsUtf8String()
                .contains("tenantContextProvider.currentTenantId() == null");
    }

    @Test
    void tenantContextProviderInjectedIntoService() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/project/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("tenant.project.application.ProjectService")
                .contentsAsUtf8String()
                .contains("TenantContextProvider tenantContextProvider");
    }

    @Test
    void repositoryHasFindByOrganisationIdMethod() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/project/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("tenant.project.application.ProjectRepository")
                .contentsAsUtf8String()
                .contains("findByOrganisationId");
    }

    @Test
    void compilationFailsIfMultipleTenantIdsDeclared() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/multiple/source/Product.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@TenantId");
    }

    @Test
    void dbMigrationGeneratesNotNullForTenantIdColumn() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/dbmigration/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"organisation_id\" VARCHAR (255) NOT NULL");
    }

    @Test
    void dbMigrationGeneratesIndexForTenantIdColumn() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/dbmigration/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE INDEX \"project_organisation_id_idx\" ON \"project\" (\"organisation_id\")");
    }
}

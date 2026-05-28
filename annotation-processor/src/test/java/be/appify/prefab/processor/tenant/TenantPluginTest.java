package be.appify.prefab.processor.tenant;

import be.appify.prefab.processor.PrefabProcessor;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class TenantPluginTest {

    @Test
    void tenantIdFieldNotInGeneratedCreateRequestRecord() {
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
    void createServicePopulatesTenantIdFromProvider() {
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
    void createServiceReconstructsAggregateWithTenantId() {
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
    void getByIdServiceFiltersByTenantId() {
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
    void getListServiceFiltersByTenantId() {
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
    void updateServiceFiltersByTenantId() {
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
    void deleteServiceFiltersByTenantId() {
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
    void tenantContextProviderInjectedIntoService() {
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
    void repositoryHasFindByOrganisationIdMethod() {
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
    void compilationFailsIfTenantIdIsNullable() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/nullable/source/Product.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@TenantId");
    }

    @Test
    void compilationFailsIfMultipleTenantIdsDeclared() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/multiple/source/Product.java"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@TenantId");
    }

    @Test
    void dbMigrationGeneratesNotNullForTenantIdColumn() {
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
    void dbMigrationGeneratesIndexForTenantIdColumn() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("tenant/dbmigration/source/Project.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE INDEX \"project_organisation_id_ix\" ON \"project\" (\"organisation_id\")");
    }
}

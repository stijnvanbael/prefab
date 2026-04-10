package be.appify.prefab.processor.audit;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AuditPluginTest {

    @Test
    void auditFieldsAreIncludedInResponseRecord() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("audit.infrastructure.http.ContractResponse")
                .contentsAsUtf8String()
                .contains("Instant createdAt");
        assertThat(compilation)
                .generatedSourceFile("audit.infrastructure.http.ContractResponse")
                .contentsAsUtf8String()
                .contains("String createdBy");
        assertThat(compilation)
                .generatedSourceFile("audit.infrastructure.http.ContractResponse")
                .contentsAsUtf8String()
                .contains("Instant lastModifiedAt");
        assertThat(compilation)
                .generatedSourceFile("audit.infrastructure.http.ContractResponse")
                .contentsAsUtf8String()
                .contains("String lastModifiedBy");
    }

    @Test
    void auditFieldsAreAbsentFromCreateRequestRecord() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("audit.application.CreateContractRequest")
                .contentsAsUtf8String()
                .doesNotContain("createdAt");
        assertThat(compilation)
                .generatedSourceFile("audit.application.CreateContractRequest")
                .contentsAsUtf8String()
                .doesNotContain("createdBy");
        assertThat(compilation)
                .generatedSourceFile("audit.application.CreateContractRequest")
                .contentsAsUtf8String()
                .doesNotContain("lastModifiedAt");
        assertThat(compilation)
                .generatedSourceFile("audit.application.CreateContractRequest")
                .contentsAsUtf8String()
                .doesNotContain("lastModifiedBy");
    }

    @Test
    void auditFieldsAreAbsentFromUpdateRequestRecord() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("audit.application.ContractUpdateRequest")
                .contentsAsUtf8String()
                .doesNotContain("createdAt");
        assertThat(compilation)
                .generatedSourceFile("audit.application.ContractUpdateRequest")
                .contentsAsUtf8String()
                .doesNotContain("lastModifiedAt");
    }

    @Test
    void createServiceMethodCallsWithAuditCreate() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("withAuditCreate");
    }

    @Test
    void updateServiceMethodCallsWithAuditUpdate() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("withAuditUpdate");
    }

    @Test
    void auditContextProviderIsInjectedIntoService() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("AuditContextProvider");
    }

    @Test
    void auditFieldsGenerateDbMigrationColumns() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"created_at\" TIMESTAMP");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"created_by\" VARCHAR");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"last_modified_at\" TIMESTAMP");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"last_modified_by\" VARCHAR");
    }
}

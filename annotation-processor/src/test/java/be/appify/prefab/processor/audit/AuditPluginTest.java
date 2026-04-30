package be.appify.prefab.processor.audit;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AuditPluginTest {

    @Test
    void auditContextProviderIsInjectedIntoService() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("AuditContextProvider");
    }

    @Test
    void createdAtIsPopulatedOnCreate() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("Instant.now()");
    }

    @Test
    void createdByIsPopulatedOnCreate() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("auditContextProvider.currentUserId()");
    }

    @Test
    void lastModifiedFieldsAreUpdatedOnUpdate() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        // Update method should reference createdAt/createdBy preserved and lastModifiedAt/lastModifiedBy updated
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("aggregate.createdAt()");
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("aggregate.createdBy()");
    }

    @Test
    void auditFieldsAreExcludedFromCreateRequest() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.CreateContractRequest")
                .contentsAsUtf8String()
                .doesNotContain("createdAt");
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.CreateContractRequest")
                .contentsAsUtf8String()
                .doesNotContain("lastModifiedAt");
    }

    @Test
    void auditFieldsAreIncludedInResponse() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/audit/source/Contract.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.audit.infrastructure.http.ContractResponse")
                .contentsAsUtf8String()
                .contains("createdAt");
        assertThat(compilation)
                .generatedSourceFile("rest.audit.infrastructure.http.ContractResponse")
                .contentsAsUtf8String()
                .contains("lastModifiedAt");
    }

    @Test
    void auditInfoValueObjectIsPopulatedOnCreate() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/audit/source/Invoice.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("new AuditInfo(");
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("Instant.now()");
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("auditContextProvider.currentUserId()");
    }

    @Test
    void auditInfoCreatedFieldsArePreservedOnUpdate() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("rest/audit/source/Invoice.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("aggregate.audit().createdAt()");
        assertThat(compilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("aggregate.audit().createdBy()");
    }
}

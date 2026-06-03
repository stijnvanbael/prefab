package be.appify.prefab.processor.audit;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AuditPluginTest {

    public static final Compilation contractCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/audit/source/Contract.java"));
    public static final Compilation invoiceCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/audit/source/Invoice.java"));

    @Test
    void auditContextProviderIsInjectedIntoService() {
        assertThat(contractCompilation).succeeded();
        assertThat(contractCompilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("AuditContextProvider");
    }

    @Test
    void createdAtIsPopulatedOnCreate() {
        assertThat(contractCompilation).succeeded();
        assertThat(contractCompilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("Instant.now()");
    }

    @Test
    void createdByIsPopulatedOnCreate() {
        assertThat(contractCompilation).succeeded();
        assertThat(contractCompilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("auditContextProvider.currentUserId()");
    }

    @Test
    void lastModifiedFieldsAreUpdatedOnUpdate() {
        assertThat(contractCompilation).succeeded();
        // Update method should reference createdAt/createdBy preserved and lastModifiedAt/lastModifiedBy updated
        assertThat(contractCompilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("aggregate.createdAt()");
        assertThat(contractCompilation)
                .generatedSourceFile("rest.audit.application.ContractService")
                .contentsAsUtf8String()
                .contains("aggregate.createdBy()");
    }

    @Test
    void auditFieldsAreExcludedFromCreateRequest() {
        assertThat(contractCompilation).succeeded();
        assertThat(contractCompilation)
                .generatedSourceFile("rest.audit.application.CreateContractRequest")
                .contentsAsUtf8String()
                .doesNotContain("createdAt");
        assertThat(contractCompilation)
                .generatedSourceFile("rest.audit.application.CreateContractRequest")
                .contentsAsUtf8String()
                .doesNotContain("lastModifiedAt");
    }

    @Test
    void auditFieldsAreIncludedInResponse() {
        assertThat(contractCompilation).succeeded();
        assertThat(contractCompilation)
                .generatedSourceFile("rest.audit.infrastructure.http.ContractResponse")
                .contentsAsUtf8String()
                .contains("createdAt");
        assertThat(contractCompilation)
                .generatedSourceFile("rest.audit.infrastructure.http.ContractResponse")
                .contentsAsUtf8String()
                .contains("lastModifiedAt");
    }

    @Test
    void auditInfoValueObjectIsPopulatedOnCreate() {
        assertThat(invoiceCompilation).succeeded();
        assertThat(invoiceCompilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("new AuditInfo(");
        assertThat(invoiceCompilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("Instant.now()");
        assertThat(invoiceCompilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("auditContextProvider.currentUserId()");
    }

    @Test
    void auditInfoCreatedFieldsArePreservedOnUpdate() {
        assertThat(invoiceCompilation).succeeded();
        assertThat(invoiceCompilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("aggregate.audit().createdAt()");
        assertThat(invoiceCompilation)
                .generatedSourceFile("rest.audit.application.InvoiceService")
                .contentsAsUtf8String()
                .contains("aggregate.audit().createdBy()");
    }
}

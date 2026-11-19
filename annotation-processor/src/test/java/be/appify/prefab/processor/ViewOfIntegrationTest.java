package be.appify.prefab.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

class ViewOfIntegrationTest {

    @Test
    void generatesFieldMappedViewEndpointsAndTestSupport() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("""
                                package test.view;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.rest.GetById;
                                import be.appify.prefab.core.annotations.rest.GetList;
                                import be.appify.prefab.core.annotations.rest.Filter;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate
                                @GetById
                                @GetList
                                public record Item(@Id String id, @Version long version, @Filter String name) {}
                                """, "test.view.Item"),
                        sourceOf("""
                                package test.view;

                                import be.appify.prefab.core.annotations.rest.GetById;
                                import be.appify.prefab.core.annotations.rest.GetList;
                                import be.appify.prefab.core.annotations.rest.ViewOf;

                                @ViewOf(Item.class)
                                @GetById
                                @GetList
                                public record ItemSummary(String name) {}
                                """, "test.view.ItemSummary"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.view.application.ItemSummaryService")
                .contentsAsUtf8String()
                .contains("return new ItemSummary(aggregate.name())");
        assertThat(compilation)
                .generatedSourceFile("test.view.application.ItemSummaryService")
                .contentsAsUtf8String()
                .contains("itemService.getList(pageable, name)");
        assertThat(compilation)
                .generatedSourceFile("test.view.infrastructure.http.ItemSummaryController")
                .contentsAsUtf8String()
                .contains("service.getList(pageable, principal, name)");
        assertThat(compilation)
                .generatedSourceFile("test.view.infrastructure.http.ItemSummaryController")
                .contentsAsUtf8String()
                .contains("service.getById(id, principal)");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/view/ItemSummaryClient.java")
                .contentsAsUtf8String()
                .contains("class ItemSummaryClient");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/view/ItemSummaryClient.java")
                .contentsAsUtf8String()
                .contains("findItemSummaries");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/view/ItemSummaryClient.java")
                .contentsAsUtf8String()
                .contains("request.queryParam(\"name\", String.valueOf(name))");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/view/ItemSummaryMother.java")
                .contentsAsUtf8String()
                .contains("class ItemSummaryMother");
    }

    @Test
    void usesBatchAssemblerWhenFieldMappingIsNotAvailable() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("""
                                package test.viewassembler;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.Exposure;
                                import be.appify.prefab.core.annotations.rest.GetList;
                                import org.springframework.data.annotation.Id;

                                @Aggregate(exposure = Exposure.INTERNAL)
                                @GetList
                                public record Item(@Id String id, String label) {}
                                """, "test.viewassembler.Item"),
                        sourceOf("""
                                package test.viewassembler;

                                import be.appify.prefab.core.annotations.rest.GetList;
                                import be.appify.prefab.core.annotations.rest.ViewOf;

                                @ViewOf(Item.class)
                                @GetList
                                public record ItemSummary(String title) {}
                                """, "test.viewassembler.ItemSummary"),
                        sourceOf("""
                                package test.viewassembler;

                                import be.appify.prefab.core.view.ViewAssembler;
                                import be.appify.prefab.core.view.ViewContext;
                                import java.util.List;
                                import org.springframework.stereotype.Component;

                                @Component
                                public class ItemSummaryAssembler implements ViewAssembler<Item, ItemSummary> {
                                    public List<ItemSummary> assemble(List<Item> items, ViewContext context) {
                                        return items.stream().map(item -> new ItemSummary(item.label())).toList();
                                    }
                                }
                                """, "test.viewassembler.ItemSummaryAssembler"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.viewassembler.application.ItemSummaryService")
                .contentsAsUtf8String()
                .contains("assembler.assemble(page.getContent()");
        assertThat(compilation)
                .generatedSourceFile("test.viewassembler.application.ItemSummaryService")
                .contentsAsUtf8String()
                .contains("ViewContext.of(principal, tenantContextProvider.currentTenantId())");
        assertThat(compilation)
                .generatedSourceFile("test.viewassembler.application.ItemSummaryService")
                .contentsAsUtf8String()
                .contains("page.getTotalElements()");
    }

    @Test
    void reportsUnknownAndMismatchedFieldsWithoutAMapper() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("""
                                package test.viewinvalid;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.rest.GetList;
                                import org.springframework.data.annotation.Id;

                                @Aggregate
                                @GetList
                                public record Item(@Id String id, int count) {}
                                """, "test.viewinvalid.Item"),
                        sourceOf("""
                                package test.viewinvalid;

                                import be.appify.prefab.core.annotations.rest.GetList;
                                import be.appify.prefab.core.annotations.rest.ViewOf;

                                @ViewOf(Item.class)
                                @GetList
                                public record ItemSummary(String missing, String count) {}
                                """, "test.viewinvalid.ItemSummary"));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("no aggregate field named 'missing'");
        assertThat(compilation).hadErrorContaining("aggregate field type int does not match view field type String");
        assertThat(compilation).hadErrorContaining("ViewAssembler<Item, ItemSummary>");
    }

    @Test
    void internalAggregateKeepsServiceOperationsButSuppressesHttpExposure() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("""
                        package test.internal;

                        import be.appify.prefab.core.annotations.Aggregate;
                        import be.appify.prefab.core.annotations.Exposure;
                        import be.appify.prefab.core.annotations.rest.Autocomplete;
                        import be.appify.prefab.core.annotations.rest.GetById;
                        import be.appify.prefab.core.annotations.rest.GetList;
                        import org.springframework.data.annotation.Id;

                        @Aggregate(exposure = Exposure.INTERNAL)
                        @GetById
                        @GetList
                        public record Item(@Id String id, @Autocomplete String name) {}
                        """, "test.internal.Item"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.internal.application.ItemService")
                .contentsAsUtf8String()
                .contains("getById(String id)");
        assertThat(compilation)
                .generatedSourceFile("test.internal.application.ItemService")
                .contentsAsUtf8String()
                .contains("getList(Pageable pageable)");
        assertThat(compilation)
                .generatedSourceFile("test.internal.infrastructure.http.ItemController")
                .contentsAsUtf8String()
                .doesNotContain("getById(");
        assertThat(compilation)
                .generatedSourceFile("test.internal.infrastructure.http.ItemController")
                .contentsAsUtf8String()
                .doesNotContain("getList(");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/internal/ItemClient.java")
                .contentsAsUtf8String()
                .doesNotContain("getItem(");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/internal/ItemClient.java")
                .contentsAsUtf8String()
                .doesNotContain("findItems(");
        assertThat(compilation)
                .generatedSourceFile("test.internal.infrastructure.http.ItemController")
                .contentsAsUtf8String()
                .doesNotContain("autocompleteByName(");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/internal/ItemClient.java")
                .contentsAsUtf8String()
                .doesNotContain("autocompleteByName(");
    }

    @Test
    void operationExposureOverridesInternalAggregateDefault() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("""
                        package test.internaloverride;

                        import be.appify.prefab.core.annotations.Aggregate;
                        import be.appify.prefab.core.annotations.Exposure;
                        import be.appify.prefab.core.annotations.rest.Create;
                        import be.appify.prefab.core.annotations.rest.GetById;
                        import org.springframework.data.annotation.Id;

                        @Aggregate(exposure = Exposure.INTERNAL)
                        @GetById(exposure = Exposure.PUBLIC)
                        public record Item(@Id String id, String name) {
                            @Create(exposure = Exposure.PUBLIC)
                            public Item(String name) { this("generated", name); }
                        }
                        """, "test.internaloverride.Item"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.internaloverride.infrastructure.http.ItemController")
                .contentsAsUtf8String()
                .contains("getById(");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/internaloverride/ItemClient.java")
                .contentsAsUtf8String()
                .contains("getItemById(");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/internaloverride/ItemClient.java")
                .contentsAsUtf8String()
                .contains("createItem(");
    }

    @Test
    void generatesEndpointsOnlyForAnnotatedViewCommandMethods() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("""
                                package test.viewcommands;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.Exposure;
                                import org.springframework.data.annotation.Id;

                                @Aggregate(exposure = Exposure.INTERNAL)
                                public record Item(@Id String id, String name) {}
                                """, "test.viewcommands.Item"),
                        sourceOf("""
                                package test.viewcommands;

                                import be.appify.prefab.core.annotations.rest.Create;
                                import be.appify.prefab.core.annotations.rest.Delete;
                                import be.appify.prefab.core.view.ViewCommands;
                                import org.springframework.stereotype.Component;

                                @Component
                                public class ItemEditor implements ViewCommands<Item> {
                                    @Create
                                    public Item create(String name) {
                                        return new Item("new", name);
                                    }

                                    @Delete
                                    public void delete(String id) {}

                                    public void internalHelper() {}
                                }
                                """, "test.viewcommands.ItemEditor"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.viewcommands.application.ItemEditorService")
                .contentsAsUtf8String()
                .contains("commands.create(name)");
        assertThat(compilation)
                .generatedSourceFile("test.viewcommands.application.ItemEditorService")
                .contentsAsUtf8String()
                .contains("commands.delete(id)");
        assertThat(compilation)
                .generatedSourceFile("test.viewcommands.infrastructure.http.ItemEditorController")
                .contentsAsUtf8String()
                .contains("create(");
        assertThat(compilation)
                .generatedSourceFile("test.viewcommands.infrastructure.http.ItemEditorController")
                .contentsAsUtf8String()
                .contains("delete(");
        assertThat(compilation)
                .generatedSourceFile("test.viewcommands.infrastructure.http.ItemEditorController")
                .contentsAsUtf8String()
                .doesNotContain("internalHelper(");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/viewcommands/ItemEditorClient.java")
                .contentsAsUtf8String()
                .contains("create(String name)");
    }

    @Test
    void supportsSeveralViewsAndConstructorOrFactoryMapping() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("""
                                package test.multiview;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.Exposure;
                                import be.appify.prefab.core.annotations.rest.GetById;
                                import org.springframework.data.annotation.Id;

                                @Aggregate(exposure = Exposure.INTERNAL)
                                @GetById
                                public record Item(@Id String id, String label) {}
                                """, "test.multiview.Item"),
                        sourceOf("""
                                package test.multiview;

                                import be.appify.prefab.core.annotations.rest.GetById;
                                import be.appify.prefab.core.annotations.rest.ViewOf;

                                @ViewOf(Item.class)
                                @GetById
                                public record ItemLabel(String display) {
                                    public ItemLabel(Item item) { this(item.label()); }
                                }
                                """, "test.multiview.ItemLabel"),
                        sourceOf("""
                                package test.multiview;

                                import be.appify.prefab.core.annotations.rest.GetById;
                                import be.appify.prefab.core.annotations.rest.ViewOf;

                                @ViewOf(Item.class)
                                @GetById
                                public record ItemTitle(String title) {
                                    public static ItemTitle from(Item item) { return new ItemTitle(item.label()); }
                                }
                                """, "test.multiview.ItemTitle"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.multiview.application.ItemLabelService")
                .contentsAsUtf8String()
                .contains("return new ItemLabel(aggregate)");
        assertThat(compilation)
                .generatedSourceFile("test.multiview.application.ItemTitleService")
                .contentsAsUtf8String()
                .contains("return ItemTitle.from(aggregate)");
        assertThat(compilation)
                .generatedSourceFile("test.multiview.infrastructure.http.ItemController")
                .contentsAsUtf8String()
                .doesNotContain("ItemLabel");
    }

    @Test
    void viewOverInternalAggregateUsesItsOwnEndpointSecurity() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("""
                                package test.internalview;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.Exposure;
                                import be.appify.prefab.core.annotations.rest.GetList;
                                import org.springframework.data.annotation.Id;

                                @Aggregate(exposure = Exposure.INTERNAL)
                                @GetList(exposure = Exposure.INTERNAL)
                                public record Item(@Id String id, String label) {}
                                """, "test.internalview.Item"),
                        sourceOf("""
                                package test.internalview;

                                import be.appify.prefab.core.annotations.rest.GetList;
                                import be.appify.prefab.core.annotations.rest.Security;
                                import be.appify.prefab.core.annotations.rest.ViewOf;

                                @ViewOf(Item.class)
                                @GetList(security = @Security(role = "DASHBOARD"))
                                public record ItemDashboard(String label) {}
                                """, "test.internalview.ItemDashboard"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.internalview.infrastructure.http.ItemDashboardController")
                .contentsAsUtf8String()
                .contains("hasRole('DASHBOARD')");
    }

    @Test
    void createFormDelegatesThroughItsGeneratedRequestMapper() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("""
                                package test.viewform;

                                import be.appify.prefab.core.annotations.Aggregate;
                                import be.appify.prefab.core.annotations.rest.Create;
                                import org.springframework.data.annotation.Id;
                                import org.springframework.data.annotation.Version;

                                @Aggregate
                                public record Item(@Id String id, @Version long version, String name) {
                                    @Create
                                    public Item(String name) { this("generated", 0L, name); }
                                }
                                """, "test.viewform.Item"),
                        sourceOf("""
                                package test.viewform;

                                import be.appify.prefab.core.annotations.rest.Create;
                                import be.appify.prefab.core.annotations.rest.ViewOf;
                                import test.viewform.application.CreateItemRequest;

                                @ViewOf(Item.class)
                                @Create
                                public record ItemForm(String name) {
                                    public CreateItemRequest toRequest() { return new CreateItemRequest(name); }
                                }
                                """, "test.viewform.ItemForm"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.viewform.application.ItemFormService")
                .contentsAsUtf8String()
                .contains("aggregateService.create(form.toRequest())");
        assertThat(compilation)
                .generatedSourceFile("test.viewform.infrastructure.http.ItemFormController")
                .contentsAsUtf8String()
                .contains("service.create(form)");
        assertThat(compilation)
                .generatedFile(javax.tools.StandardLocation.CLASS_OUTPUT, "test/viewform/ItemFormClient.java")
                .contentsAsUtf8String()
                .contains("jsonMapper.writeValueAsString(form)");
    }

    private static JavaFileObject sourceOf(String sourceCode, String fileName) {
        return JavaFileObjects.forSourceString(fileName, sourceCode);
    }
}

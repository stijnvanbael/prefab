package be.appify.prefab.processor;

import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_BUILDERS;
import static be.appify.prefab.processor.TestClasses.SECURITY_MOCK_MVC_CONFIGURERS;

class TestClientWriter {
    private final TestJavaFileWriter fileWriter;
    private final PrefabContext context;

    TestClientWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new TestJavaFileWriter(context, null);
    }

    void writeTestSupport(ClassManifest manifest) {
        writeTestClient(manifest);
    }

    private void writeTestClient(ClassManifest manifest) {
        var className = "%sClient" .formatted(manifest.simpleName());
        var type = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(MOCK_MVC, "mockMvc")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addField(FieldSpec.builder(JsonMapper.class, "jsonMapper")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(WebApplicationContext.class, "context")
                        .addParameter(JsonMapper.class, "jsonMapper")
                        .addStatement("""
                                        this.mockMvc = $T
                                            .webAppContextSetup(context)$L
                                            .build()""",
                                MOCK_MVC_BUILDERS,
                                ControllerUtil.SECURITY_INCLUDED
                                        ? CodeBlock.of("\n.apply($T.springSecurity())", SECURITY_MOCK_MVC_CONFIGURERS)
                                        : CodeBlock.of(""))
                        .addStatement("this.jsonMapper = jsonMapper")
                        .build());
        context.plugins().forEach(plugin -> plugin.writeTestClient(manifest, type));

        fileWriter.writeFile(manifest.packageName(), className, type.build());
    }
}

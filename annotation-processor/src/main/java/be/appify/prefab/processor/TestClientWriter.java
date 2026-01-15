package be.appify.prefab.processor;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

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
        var className = "%sClient".formatted(manifest.simpleName());
        var type = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(MockMvc.class, "mockMvc")
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
                                            .webAppContextSetup(context)
                                            .apply($T.springSecurity())
                                            .build()""",
                                MockMvcBuilders.class,
                                SecurityMockMvcConfigurers.class)
                        .addStatement("this.jsonMapper = jsonMapper")
                        .build());
        context.plugins().forEach(plugin -> plugin.writeTestClient(manifest, type, context));

        fileWriter.writeFile(manifest.packageName(), className, type.build());
    }
}

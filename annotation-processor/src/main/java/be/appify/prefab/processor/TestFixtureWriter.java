package be.appify.prefab.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.lang.model.element.Modifier;

class TestFixtureWriter {
    private final TestJavaFileWriter fileWriter;
    private final PrefabContext context;

    TestFixtureWriter(PrefabContext context) {
        this.context = context;
        fileWriter = new TestJavaFileWriter(context, null);
    }

    void writeTestSupport(ClassManifest manifest) {
        writeTestFixture(manifest);
    }

    private void writeTestFixture(ClassManifest manifest) {
        var className = "%sFixture".formatted(manifest.simpleName());
        var type = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(MockMvc.class, "mockMvc")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addField(FieldSpec.builder(ObjectMapper.class, "objectMapper")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(WebApplicationContext.class, "context")
                        .addParameter(ObjectMapper.class, "objectMapper")
                        .addStatement("""
                                        this.mockMvc = $T
                                            .webAppContextSetup(context)
                                            .apply($T.springSecurity())
                                            .build()""",
                                MockMvcBuilders.class,
                                SecurityMockMvcConfigurers.class)
                        .addStatement("this.objectMapper = objectMapper")
                        .build());
        context.plugins().forEach(plugin -> plugin.writeTestFixture(manifest, type, context));

        fileWriter.writeFile(manifest.packageName(), className, type.build());
    }
}

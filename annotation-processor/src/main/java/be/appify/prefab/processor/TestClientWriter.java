package be.appify.prefab.processor;

import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static be.appify.prefab.processor.TestClasses.ANNOTATION_AWARE_ORDER_COMPARATOR;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_CONFIGURER;
import static be.appify.prefab.processor.TestClasses.SECURITY_MOCK_MVC_CONFIGURERS;

class TestClientWriter {
    private final TestFileOutput fileWriter;
    private final PrefabContext context;

    TestClientWriter(PrefabContext context) {
        this(context, new TestJavaFileWriter(context, null));
    }

    TestClientWriter(PrefabContext context, TestFileOutput fileWriter) {
        this.context = context;
        this.fileWriter = fileWriter;
    }

    void writeTestSupport(ClassManifest manifest) {
        writeTestClient(manifest);
    }

    void writePolymorphicTestSupport(PolymorphicAggregateManifest manifest) {
        writePolymorphicTestClient(manifest);
    }

    private void writeTestClient(ClassManifest manifest) {
        var className = "%sClient".formatted(manifest.simpleName());
        var type = buildClientType(className);
        context.plugins().forEach(plugin -> plugin.writeTestClient(manifest, type));
        fileWriter.setPreferredElement(manifest.type().asElement());
        fileWriter.writeFile(manifest.packageName(), className, type.build());
    }

    private void writePolymorphicTestClient(PolymorphicAggregateManifest manifest) {
        var className = "%sClient".formatted(manifest.simpleName());
        var type = buildClientType(className);
        context.plugins().forEach(plugin -> plugin.writePolymorphicTestClient(manifest, type));
        fileWriter.setPreferredElement(manifest.type().asElement());
        fileWriter.writeFile(manifest.packageName(), className, type.build());
    }

    private TypeSpec.Builder buildClientType(String className) {
        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(MOCK_MVC, "mockMvc")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addField(FieldSpec.builder(JsonMapper.class, "jsonMapper")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addMethod(buildConstructor());
    }

    static MethodSpec buildConstructor() {
        var configurersType = ParameterizedTypeName.get(ClassName.get(List.class), MOCK_MVC_CONFIGURER);
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(WebApplicationContext.class, "context")
                .addParameter(JsonMapper.class, "jsonMapper")
                .addParameter(configurersType, "configurers")
                .addStatement("var builder = $T.webAppContextSetup(context)", MOCK_MVC_BUILDERS);
        applyConfigurers(constructor);
        return constructor
                .addStatement("this.mockMvc = builder.build()")
                .addStatement("this.jsonMapper = jsonMapper")
                .build();
    }

    private static void applyConfigurers(MethodSpec.Builder constructor) {
        if (ControllerUtil.SECURITY_INCLUDED) {
            constructor
                    .beginControlFlow("if (configurers.isEmpty())")
                    .addStatement("builder.apply($T.springSecurity())", SECURITY_MOCK_MVC_CONFIGURERS)
                    .nextControlFlow("else")
                    .addStatement("$T.sort(configurers)", ANNOTATION_AWARE_ORDER_COMPARATOR)
                    .addStatement("configurers.forEach(builder::apply)")
                    .endControlFlow();
        } else {
            constructor
                    .addStatement("$T.sort(configurers)", ANNOTATION_AWARE_ORDER_COMPARATOR)
                    .addStatement("configurers.forEach(builder::apply)");
        }
    }
}

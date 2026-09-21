package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.processor.rest.ControllerUtil;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Modifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static be.appify.prefab.processor.TestClasses.ANNOTATION_AWARE_ORDER_COMPARATOR;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_CONFIGURER;
import static be.appify.prefab.processor.TestClasses.REQUEST_POST_PROCESSOR;

public class TestClientWriter {
    private final FileOutput fileWriter;
    private final PrefabContext context;

    TestClientWriter(PrefabContext context) {
        this(context, new OutputTargetFileOutput(context, null, OutputTarget.TEST));
    }

    public TestClientWriter(PrefabContext context, FileOutput fileWriter) {
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
        var type = buildClientType(manifest.packageName(), className);
        applyPlugins(manifest.type().asElement(), plugin -> plugin.writeTestClient(manifest, type));
        fileWriter.setPreferredElement(manifest.type().asElement());
        fileWriter.writeFile(manifest.packageName(), className, type.build());
    }

    private void writePolymorphicTestClient(PolymorphicAggregateManifest manifest) {
        var className = "%sClient".formatted(manifest.simpleName());
        var type = buildClientType(manifest.packageName(), className);
        applyPlugins(manifest.type().asElement(), plugin -> plugin.writePolymorphicTestClient(manifest, type));
        fileWriter.setPreferredElement(manifest.type().asElement());
        fileWriter.writeFile(manifest.packageName(), className, type.build());
    }

    private TypeSpec.Builder buildClientType(String packageName, String className) {
        var type = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(FieldSpec.builder(MOCK_MVC, "mockMvc")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                .addField(FieldSpec.builder(JsonMapper.class, "jsonMapper")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build());
        if (ControllerUtil.SECURITY_INCLUDED) {
            var selfType = ClassName.get(packageName, className);
            type.addField(FieldSpec.builder(securityOverridesType(), "securityOverrides")
                            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                            .build())
                    .addMethod(buildConstructor())
                    .addMethod(buildSecurityOverrideConstructor())
                    .addMethod(buildAsMethod(selfType))
                    .addMethod(buildApplySecurityOverrideMethod());
        } else {
            type.addMethod(buildConstructor());
        }
        return type;
    }

    static MethodSpec buildConstructor() {
        var configurersType = ParameterizedTypeName.get(ClassName.get(List.class), MOCK_MVC_CONFIGURER);
        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(WebApplicationContext.class, "context")
                .addParameter(JsonMapper.class, "jsonMapper")
                .addParameter(configurersType, "configurers")
                .addStatement("var builder = $T.webAppContextSetup(context)", MOCK_MVC_BUILDERS)
                .addStatement("$T.sort(configurers)", ANNOTATION_AWARE_ORDER_COMPARATOR)
                .addStatement("configurers.forEach(builder::apply)")
                .addStatement("this.mockMvc = builder.build()")
                .addStatement("this.jsonMapper = jsonMapper");
        if (ControllerUtil.SECURITY_INCLUDED) {
            constructor.addStatement("this.securityOverrides = $T.of()", ClassName.get(List.class));
        }
        return constructor.build();
    }

    private static MethodSpec buildSecurityOverrideConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(MOCK_MVC, "mockMvc")
                .addParameter(JsonMapper.class, "jsonMapper")
                .addParameter(securityOverridesType(), "securityOverrides")
                .addStatement("this.mockMvc = mockMvc")
                .addStatement("this.jsonMapper = jsonMapper")
                .addStatement("this.securityOverrides = securityOverrides")
                .build();
    }

    private static MethodSpec buildAsMethod(ClassName selfType) {
        return MethodSpec.methodBuilder("as")
                .addModifiers(Modifier.PUBLIC)
                .varargs()
                .returns(selfType)
                .addParameter(ArrayTypeName.of(REQUEST_POST_PROCESSOR), "requestPostProcessors")
                .addStatement("return new $T(mockMvc, jsonMapper, $T.of(requestPostProcessors))", selfType, ClassName.get(List.class))
                .build();
    }

    private static MethodSpec buildApplySecurityOverrideMethod() {
        return MethodSpec.methodBuilder("applySecurityOverride")
                .addModifiers(Modifier.PRIVATE)
                .returns(REQUEST_POST_PROCESSOR)
                .addParameter(REQUEST_POST_PROCESSOR, "defaultPostProcessor")
                .beginControlFlow("if (securityOverrides.isEmpty())")
                .addStatement("return defaultPostProcessor")
                .endControlFlow()
                .addStatement("""
                        return request -> {
                            for ($T override : securityOverrides) {
                                request = override.postProcessRequest(request);
                            }
                            return request;
                        }""", REQUEST_POST_PROCESSOR)
                .build();
    }

    private static ParameterizedTypeName securityOverridesType() {
        return ParameterizedTypeName.get(ClassName.get(List.class), REQUEST_POST_PROCESSOR);
    }

    private void applyPlugins(TypeElement aggregateType, java.util.function.Consumer<PrefabPlugin> action) {
        context.plugins().stream()
                .filter(plugin -> context.isPluginEnabledFor(aggregateType, plugin.getClass()))
                .filter(plugin -> context.getOutputTargetFor(aggregateType, plugin.getClass()) != OutputTarget.MAIN)
                .forEach(plugin -> context.withPluginOutputTarget(aggregateType, plugin.getClass(), () -> action.accept(plugin)));
    }
}


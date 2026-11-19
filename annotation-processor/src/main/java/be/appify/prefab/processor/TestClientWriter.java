package be.appify.prefab.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.spring.Page;
import be.appify.prefab.processor.rest.ControllerUtil;
import be.appify.prefab.processor.rest.getlist.GetListUtil;
import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Modifier;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static be.appify.prefab.processor.TestClasses.ANNOTATION_AWARE_ORDER_COMPARATOR;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_CONFIGURER;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;
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

    void writeViewTestSupport(
            TypeElement viewElement,
            ClassManifest view,
            String basePath,
            GetById getById,
            GetList getList,
            List<GetListUtil.FilterManifest> filters
    ) {
        var className = "%sClient".formatted(view.simpleName());
        var type = buildClientType(view.packageName(), className);
        if (getById != null) {
            type.addMethod(viewGetByIdMethod(view, basePath, getById));
        }
        if (getList != null) {
            type.addMethod(viewGetListMethod(view, basePath, getList, filters));
        }
        fileWriter.setPreferredElement(viewElement);
        fileWriter.writeFile(view.packageName(), className, type.build());
    }

    void writeViewCommandTestSupport(ViewCommandWriter.CommandModel model) {
        var commandType = model.commandType();
        var className = commandType.getSimpleName() + "Client";
        var type = buildClientType(packageNameOf(commandType), className);
        model.operations().stream()
                .filter(ViewCommandWriter::isPublic)
                .forEach(operation -> type.addMethod(viewCommandMethod(operation, commandType)));
        fileWriter.setPreferredElement(commandType);
        fileWriter.writeFile(packageNameOf(commandType), className, type.build());
    }

    void writeViewCreateFormTestSupport(
            TypeElement formElement,
            ClassManifest form,
            Create create
    ) {
        var className = form.simpleName() + "Client";
        var type = buildClientType(form.packageName(), className);
        type.addMethod(MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(form.type().asTypeName(), "form")
                .addException(Exception.class)
                .addStatement("""
                        mockMvc.perform($T.$N($S)$L
                                .contentType($T.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(form)))
                                .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        ViewCommandWriter.basePath(formElement) + create.path(),
                        ControllerUtil.withMockUser(create.security()),
                        org.springframework.http.MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .build());
        fileWriter.setPreferredElement(formElement);
        fileWriter.writeFile(form.packageName(), className, type.build());
    }

    private static MethodSpec viewCommandMethod(
            ViewCommandWriter.CommandOperation operation,
            TypeElement commandType
    ) {
        var method = MethodSpec.methodBuilder(operation.method().getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addException(Exception.class)
                .addParameters(operation.method().getParameters().stream()
                        .map(parameter -> com.palantir.javapoet.ParameterSpec.builder(
                                com.palantir.javapoet.TypeName.get(parameter.asType()),
                                parameter.getSimpleName().toString()).build())
                        .toList());
        var request = com.palantir.javapoet.CodeBlock.builder()
                .add("var request = $T.$N($S", MOCK_MVC_REQUEST_BUILDERS,
                        operation.httpMethod().toLowerCase(),
                        ViewCommandWriter.basePath(commandType)
                                + operation.path());
        be.appify.prefab.processor.rest.PathVariables.extractFrom(operation.path()).forEach(name ->
                request.add(", $N", name));
        request.add(")$L", ControllerUtil.withMockUser(operation.security()));
        method.addCode(request.build());
        operation.bodyParameters().stream().findFirst().ifPresent(body ->
                method.addStatement("request.contentType($T.APPLICATION_JSON).content(jsonMapper.writeValueAsString($N))",
                        org.springframework.http.MediaType.class, body));
        return method.addStatement("""
                        mockMvc.perform(request.accept($T.APPLICATION_JSON))
                                .andExpect($T.status().isOk())""",
                        org.springframework.http.MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .build();
    }

    private static String packageNameOf(TypeElement type) {
        return type.getEnclosingElement() instanceof javax.lang.model.element.PackageElement packageElement
                ? packageElement.getQualifiedName().toString()
                : "";
    }

    private static MethodSpec viewGetByIdMethod(ClassManifest view, String basePath, GetById getById) {
        var method = MethodSpec.methodBuilder("get" + view.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(view.type().asTypeName())
                .addParameter(String.class, "id")
                .addException(Exception.class)
                .addStatement("var request = $T.$N($S, id)$L",
                        MOCK_MVC_REQUEST_BUILDERS,
                        getById.method().toLowerCase(),
                        basePath + getById.path(),
                        ControllerUtil.withMockUser(getById.security()));
        return method.addStatement("""
                        var json = mockMvc.perform(request.accept($T.APPLICATION_JSON))
                                .andExpect($T.status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString()""",
                        org.springframework.http.MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return jsonMapper.readValue(json, $T.class)", view.type().asTypeName())
                .build();
    }

    private static MethodSpec viewGetListMethod(
            ClassManifest view,
            String basePath,
            GetList getList,
            List<GetListUtil.FilterManifest> filters
    ) {
        var returnType = ParameterizedTypeName.get(ClassName.get(Page.class), view.type().asTypeName());
        var method = MethodSpec.methodBuilder("find" + view.plural())
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(Pageable.class, "pageable")
                .addParameters(filters.stream()
                        .map(filter -> com.palantir.javapoet.ParameterSpec.builder(
                                GetListUtil.filterParameterType(filter), filter.parameterName()).build())
                        .toList())
                .addException(Exception.class)
                .addStatement("var request = $T.$N($S)$L",
                        MOCK_MVC_REQUEST_BUILDERS,
                        getList.method().toLowerCase(),
                        basePath + getList.path(),
                        ControllerUtil.withMockUser(getList.security()))
                .addCode("""
                        if (pageable != null && pageable.isPaged()) {
                            request.queryParam("page", String.valueOf(pageable.getPageNumber()))
                                   .queryParam("size", String.valueOf(pageable.getPageSize()));
                        }
                        if (pageable != null && pageable.getSort().isSorted()) {
                            request.queryParam("sort", $T.toRequestParams(pageable.getSort()));
                        }
                        """, ControllerUtil.class)
                        .addCode(filters.stream()
                                .map(filter -> com.palantir.javapoet.CodeBlock.of("""
                                        if ($N != null) {
                                            request.queryParam($S, String.valueOf($N));
                                        }
                                        """, filter.parameterName(), filter.parameterName(), filter.parameterName()))
                                .collect(com.palantir.javapoet.CodeBlock.joining("\n")))
                .addStatement("""
                        var json = mockMvc.perform(request.accept($T.APPLICATION_JSON))
                                .andExpect($T.status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString()""",
                        org.springframework.http.MediaType.class,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return jsonMapper.readValue(json, new $T() {})",
                        ParameterizedTypeName.get(
                                ClassName.get(tools.jackson.core.type.TypeReference.class), returnType));
        return method.build();
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
            // A second (private) constructor is present to support as(...), so Spring's implicit
            // single-constructor autowiring no longer applies; mark this one explicitly.
            constructor.addAnnotation(Autowired.class);
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

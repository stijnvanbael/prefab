package be.appify.prefab.processor.rest.createorupdate;

import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.ControllerUtil;
import be.appify.prefab.processor.rest.PathVariables;
import be.appify.prefab.processor.rest.create.CreateServiceWriter;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.springframework.http.MediaType;

import static be.appify.prefab.processor.TestClasses.MOCK_MVC_REQUEST_BUILDERS;
import static be.appify.prefab.processor.TestClasses.MOCK_MVC_RESULT_MATCHERS;
import static be.appify.prefab.processor.TestClasses.TEST_UTIL;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

class CreateOrUpdateTestClientWriter {

    List<MethodSpec> createOrUpdateMethods(ClassManifest manifest, CreateOrUpdateManifest createOrUpdate,
            PrefabContext context) {
        var create = createOrUpdate.createConstructor().getAnnotation(Create.class);
        var pathVarNames = PathVariables.extractFrom(create.path());
        var lookupVar = createOrUpdate.lookupVariable();
        var path = "/" + ControllerUtil.pathOf(manifest) + create.path();

        var params = createOrUpdate.createConstructor().getParameters().stream()
                .map(p -> VariableManifest.of(p, context.processingEnvironment()))
                .toList();
        var parentName = CreateServiceWriter.parentFieldName(manifest);
        var bodyParams = params.stream()
                .filter(p -> !pathVarNames.contains(p.name()))
                .filter(p -> parentName.map(name -> !name.equals(p.name())).orElse(true))
                .toList();

        var bodyType = ClassName.get(
                "%s.application".formatted(manifest.packageName()),
                "Create%sRequest".formatted(manifest.simpleName()));
        var requestVarName = uncapitalize(manifest.simpleName());

        if (bodyParams.isEmpty()) {
            return List.of(buildNoBodyMethod(manifest, create, lookupVar, path));
        }

        var individualBodyParams = bodyParams.stream()
                .flatMap(p -> context.requestParameterBuilder().buildTestClientParameter(p).stream())
                .toList();
        var allIndividualParams = java.util.stream.Stream.concat(
                java.util.stream.Stream.of(ParameterSpec.builder(String.class, lookupVar).build()),
                individualBodyParams.stream()).toList();

        return List.of(
                buildIndividualParamsMethod(manifest, lookupVar, allIndividualParams, bodyType,
                        individualBodyParams),
                buildRequestOverload(manifest, create, lookupVar, bodyType, requestVarName, path));
    }

    private MethodSpec buildNoBodyMethod(ClassManifest manifest, Create create, String lookupVar, String path) {
        return MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addException(Exception.class)
                .addParameter(String.class, lookupVar)
                .addStatement("""
                                var result = mockMvc.perform($T.$N($S, $L)$L)
                                        .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        path,
                        lookupVar,
                        ControllerUtil.withMockUser(create.security()),
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return $T.idOf(result)", TEST_UTIL)
                .build();
    }

    private MethodSpec buildIndividualParamsMethod(
            ClassManifest manifest,
            String lookupVar,
            List<ParameterSpec> allIndividualParams,
            ClassName bodyType,
            List<ParameterSpec> individualBodyParams
    ) {
        var bodyParamNames = individualBodyParams.stream()
                .map(ParameterSpec::name)
                .collect(Collectors.joining(", "));
        return MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameters(allIndividualParams)
                .addException(Exception.class)
                .addStatement("return create$L($L, new $T($L))", manifest.simpleName(), lookupVar, bodyType,
                        bodyParamNames)
                .build();
    }

    private MethodSpec buildRequestOverload(
            ClassManifest manifest,
            Create create,
            String lookupVar,
            ClassName bodyType,
            String requestVarName,
            String path
    ) {
        return MethodSpec.methodBuilder("create" + manifest.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addException(Exception.class)
                .addParameter(String.class, lookupVar)
                .addParameter(bodyType, requestVarName)
                .addStatement("""
                                var result = mockMvc.perform($T.$N($S, $L)$L
                                .contentType($T.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString($L)))
                                .andExpect($T.status().isCreated())""",
                        MOCK_MVC_REQUEST_BUILDERS,
                        create.method().toLowerCase(),
                        path,
                        lookupVar,
                        ControllerUtil.withMockUser(create.security()),
                        MediaType.class,
                        requestVarName,
                        MOCK_MVC_RESULT_MATCHERS)
                .addStatement("return $T.idOf(result)", TEST_UTIL)
                .build();
    }
}

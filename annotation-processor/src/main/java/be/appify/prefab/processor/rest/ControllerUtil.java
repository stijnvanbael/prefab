package be.appify.prefab.processor.rest;

import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.RequestParameterBuilder;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static be.appify.prefab.processor.CaseUtil.toKebabCase;
import static org.apache.commons.text.WordUtils.capitalize;
import static org.apache.commons.text.WordUtils.uncapitalize;
import static org.atteo.evo.inflector.English.plural;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/** Utility class for controller-related operations. */
public class ControllerUtil {

    private static final ClassName PRE_AUTHORIZE = ClassName.get("org.springframework.security.access.prepost", "PreAuthorize");
    private static final ClassName TAG = ClassName.get("io.swagger.v3.oas.annotations.tags", "Tag");
    private static final ClassName OPERATION = ClassName.get("io.swagger.v3.oas.annotations", "Operation");
    private static final ClassName PARAMETER = ClassName.get("io.swagger.v3.oas.annotations", "Parameter");
    private static final ClassName PARAMETER_IN = ClassName.get("io.swagger.v3.oas.annotations.enums", "ParameterIn");
    /** Flag to indicate if Spring Security is included in the classpath. */
    public static final boolean SECURITY_INCLUDED = isSecurityIncluded();
    /** Flag to indicate if OpenAPI annotations (io.swagger.v3.oas.annotations) are on the classpath. */
    public static final boolean OPENAPI_INCLUDED = isOpenApiIncluded();

    private static boolean isSecurityIncluded() {
        try {
            Class.forName("org.springframework.security.access.prepost.PreAuthorize");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isOpenApiIncluded() {
        try {
            Class.forName("io.swagger.v3.oas.annotations.Operation");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private ControllerUtil() {
    }

    /**
     * Generates the path for a given ClassManifest, considering its parent if present.
     *
     * @param manifest
     *         the ClassManifest for which to generate the path
     * @return the generated path as a String
     */
    public static String pathOf(ClassManifest manifest) {
        var parentPath = manifest.parent()
                .map(parent -> "%s/{%sId}/" .formatted(
                        toKebabCase(plural(parent.type().parameters().getFirst().simpleName())),
                        uncapitalize(parent.name())))
                .orElse("");
        return parentPath + toKebabCase(plural(manifest.simpleName()));
    }

    /**
     * Converts a Sort object into an array of request parameters.
     *
     * @param sort
     *         the Sort object to convert
     * @return an array of request parameters representing the sort order
     */
    public static String[] toRequestParams(Sort sort) {
        var params = new ArrayList<String>();
        sort.forEach(order -> params.add(order.getProperty() + "," + order.getDirection().name()));
        return params.toArray(new String[0]);
    }

    /**
     * Generates an Optional AnnotationSpec for security based on the provided Security annotation.
     *
     * @param security
     *         the Security annotation to evaluate
     * @return an Optional containing the AnnotationSpec if security is enabled, otherwise an empty Optional
     */
    public static Optional<AnnotationSpec> securedAnnotation(Security security) {
        if (!SECURITY_INCLUDED) {
            return Optional.empty();
        }
        if (!security.enabled()) {
            return Optional.of(AnnotationSpec.builder(PRE_AUTHORIZE)
                    .addMember("value", "$S", "permitAll()")
                    .build());
        }
        return !security.authority().isEmpty() ?
                Optional.of(AnnotationSpec.builder(PRE_AUTHORIZE)
                        .addMember("value", "$S", "hasAuthority('%s')" .formatted(security.authority()))
                        .build()) :
                Optional.of(AnnotationSpec.builder(PRE_AUTHORIZE)
                        .addMember("value", "$S", "isAuthenticated()")
                        .build());
    }

    /**
     * Generates a CodeBlock for mocking a user in tests based on the provided Security annotation.
     *
     * @param security
     *         the Security annotation to evaluate
     * @return a CodeBlock for mocking a user if security is enabled, otherwise an empty CodeBlock
     */
    public static CodeBlock withMockUser(Security security) {
        if (!SECURITY_INCLUDED) {
            return CodeBlock.of("");
        }
        return security.enabled() ?
                CodeBlock.of("\n.with($T.user(\"test\")$L)",
                        ClassName.get("org.springframework.security.test.web.servlet.request", "SecurityMockMvcRequestPostProcessors"),
                        security.authority().isEmpty()
                                ? CodeBlock.of("")
                                : CodeBlock.of(".authorities(new $T($S))",
                                        ClassName.get("org.springframework.security.core.authority", "SimpleGrantedAuthority"),
                                        security.authority()))
                : CodeBlock.of("");
    }

    /**
     * Generates an AnnotationSpec for OpenAPI @Tag to group controller endpoints.
     * Returns empty if OpenAPI annotations are not on the classpath.
     *
     * @param name
     *         the tag name (usually the domain class name)
     * @return an Optional containing the AnnotationSpec if OpenAPI is available
     */
    public static Optional<AnnotationSpec> tagAnnotation(String name) {
        if (!OPENAPI_INCLUDED) {
            return Optional.empty();
        }
        return Optional.of(AnnotationSpec.builder(TAG)
                .addMember("name", "$S", name)
                .build());
    }

    /**
     * Generates an AnnotationSpec for OpenAPI @Operation describing an endpoint.
     * Returns empty if OpenAPI annotations are not on the classpath.
     *
     * @param summary
     *         a short summary of the operation
     * @return an Optional containing the AnnotationSpec if OpenAPI is available
     */
    public static Optional<AnnotationSpec> operationAnnotation(String summary) {
        if (!OPENAPI_INCLUDED) {
            return Optional.empty();
        }
        return Optional.of(AnnotationSpec.builder(OPERATION)
                .addMember("summary", "$S", summary)
                .build());
    }

    /**
     * Generates an AnnotationSpec for OpenAPI @Parameter describing a path variable.
     * Returns empty if OpenAPI annotations are not on the classpath.
     *
     * @param description
     *         a description of the parameter
     * @return an Optional containing the AnnotationSpec if OpenAPI is available
     */
    public static Optional<AnnotationSpec> pathParameterAnnotation(String description) {
        if (!OPENAPI_INCLUDED) {
            return Optional.empty();
        }
        return Optional.of(AnnotationSpec.builder(PARAMETER)
                .addMember("description", "$S", description)
                .addMember("in", "$T.PATH", PARAMETER_IN)
                .build());
    }

    /**
     * Generates an AnnotationSpec for RequestMapping with optional multipart form data consumption.
     *
     * @param method
     *         the HTTP method for the request mapping
     * @param path
     *         the path for the request mapping
     * @param requestParts
     *         the list of request parts to determine if multipart form data is needed
     * @return the generated AnnotationSpec for RequestMapping
     */
    public static AnnotationSpec requestMapping(String method, String path, List<ParameterSpec> requestParts) {
        var requestMapping = AnnotationSpec.builder(RequestMapping.class)
                .addMember("method", "$T.$N", RequestMethod.class, method)
                .addMember("path", "$S", path);
        if (!requestParts.isEmpty()) {
            requestMapping.addMember("consumes", "$S", MULTIPART_FORM_DATA_VALUE);
        }
        return requestMapping.build();
    }

    /**
     * Generates an AnnotationSpec for RequestMapping.
     *
     * @param method
     *         the HTTP method for the request mapping
     * @param path
     *         the path for the request mapping
     * @return the generated AnnotationSpec for RequestMapping
     */
    public static AnnotationSpec requestMapping(String method, String path) {
        return AnnotationSpec.builder(RequestMapping.class)
                .addMember("method", "$T.$N", RequestMethod.class, method)
                .addMember("path", "$S", path)
                .build();
    }

    /**
     * Generates the response type ClassName for a given ClassManifest.
     *
     * @param manifest
     *         the ClassManifest for which to generate the response type
     * @return the generated response type as a ClassName
     */
    public static ClassName responseType(ClassManifest manifest) {
        return ClassName.get("%s.infrastructure.http" .formatted(manifest.packageName()),
                "%sResponse" .formatted(manifest.simpleName()));
    }

    /**
     * Generates the response type ClassName for a given PolymorphicAggregateManifest.
     *
     * @param manifest
     *         the PolymorphicAggregateManifest for which to generate the response type
     * @return the generated response type as a ClassName
     */
    public static ClassName responseType(PolymorphicAggregateManifest manifest) {
        return ClassName.get("%s.infrastructure.http" .formatted(manifest.packageName()),
                "%sResponse" .formatted(manifest.simpleName()));
    }

    /**
     * Writes a record TypeSpec based on the provided class name, fields, and parameter builder.
     *
     * @param name
     *         the ClassName for the record
     * @param fields
     *         the list of VariableManifest fields for the record
     * @param parameterBuilder
     *         the RequestParameterBuilder to build parameters
     * @return the generated TypeSpec for the record
     */
    public static TypeSpec writeRecord(
            ClassName name,
            List<VariableManifest> fields,
            RequestParameterBuilder parameterBuilder
    ) {
        var type = TypeSpec.recordBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .recordConstructor(MethodSpec.compactConstructorBuilder()
                        .addParameters(fields.stream()
                                .flatMap(param -> parameterBuilder.buildBodyParameter(param)
                                        .or(() -> parameterBuilder.buildMethodParameter(param))
                                        .stream())
                                .toList())
                        .build());
        type.addMethods(fields.stream()
                .flatMap(parameter -> parameterBuilder.buildMethodParameter(parameter).stream())
                .map(parameter -> withMethod(name, parameter, fields))
                .toList());
        return type.build();
    }

    private static MethodSpec withMethod(TypeName type, ParameterSpec parameter, List<VariableManifest> fields) {
        return MethodSpec.methodBuilder("with%s" .formatted(capitalize(parameter.name())))
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addParameter(parameter)
                .addStatement("return new $T($L)", type, fields.stream()
                        .map(VariableManifest::name)
                        .collect(java.util.stream.Collectors.joining(", ")))
                .build();
    }
}

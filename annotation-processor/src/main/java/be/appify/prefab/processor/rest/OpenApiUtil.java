package be.appify.prefab.processor.rest;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import java.util.Optional;

/**
 * Utility class for generating OpenAPI documentation annotations in generated REST controllers.
 * Annotations are only generated when springdoc-openapi is present on the classpath.
 */
public class OpenApiUtil {

    private static final ClassName TAG = ClassName.get("io.swagger.v3.oas.annotations.tags", "Tag");
    private static final ClassName OPERATION = ClassName.get("io.swagger.v3.oas.annotations", "Operation");
    private static final ClassName API_RESPONSE = ClassName.get("io.swagger.v3.oas.annotations.responses", "ApiResponse");

    /** Flag indicating whether springdoc-openapi is available on the classpath. */
    public static final boolean OPEN_API_INCLUDED = isOpenApiIncluded();

    private static boolean isOpenApiIncluded() {
        try {
            Class.forName("io.swagger.v3.oas.annotations.Operation");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private OpenApiUtil() {
    }

    /**
     * Generates a {@code @Tag} annotation spec for the given aggregate name.
     *
     * @param name the name to use as the tag
     * @return an Optional containing the annotation spec, or empty if OpenAPI is not available
     */
    public static Optional<AnnotationSpec> tagAnnotation(String name) {
        if (!OPEN_API_INCLUDED) {
            return Optional.empty();
        }
        return Optional.of(AnnotationSpec.builder(TAG)
                .addMember("name", "$S", name)
                .build());
    }

    /**
     * Generates an {@code @Operation} annotation spec with the given summary.
     *
     * @param summary the summary description of the operation
     * @return an Optional containing the annotation spec, or empty if OpenAPI is not available
     */
    public static Optional<AnnotationSpec> operationAnnotation(String summary) {
        if (!OPEN_API_INCLUDED) {
            return Optional.empty();
        }
        return Optional.of(AnnotationSpec.builder(OPERATION)
                .addMember("summary", "$S", summary)
                .build());
    }

    /**
     * Generates an {@code @ApiResponse} annotation spec for the given response code and description.
     *
     * @param responseCode  the HTTP response code (e.g., "200", "404")
     * @param description   the description of the response
     * @return an Optional containing the annotation spec, or empty if OpenAPI is not available
     */
    public static Optional<AnnotationSpec> apiResponseAnnotation(String responseCode, String description) {
        if (!OPEN_API_INCLUDED) {
            return Optional.empty();
        }
        return Optional.of(AnnotationSpec.builder(API_RESPONSE)
                .addMember("responseCode", "$S", responseCode)
                .addMember("description", "$S", description)
                .build());
    }
}

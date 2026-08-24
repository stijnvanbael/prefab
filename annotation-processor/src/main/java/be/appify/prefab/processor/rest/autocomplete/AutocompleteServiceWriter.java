package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;

import javax.lang.model.element.Modifier;
import java.util.List;

import static be.appify.prefab.processor.CaseUtil.toPascalCase;
import static org.apache.commons.text.WordUtils.uncapitalize;

class AutocompleteServiceWriter {
    private static final boolean JDBC_INCLUDED = isClassIncluded("org.springframework.data.relational.core.mapping.Table");
    private static final boolean MONGO_INCLUDED = isClassIncluded("org.springframework.data.mongodb.core.MongoTemplate");

    List<MethodSpec> autocompleteMethods(ClassManifest manifest) {
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        return manifest.fields().stream()
                .map(field -> field.getAnnotation(Autocomplete.class)
                        .map(annotation -> autocompleteMethods(
                                manifest,
                                field.name(),
                                repositoryName,
                                annotation.value().limit())))
                .flatMap(java.util.Optional::stream)
                .flatMap(List::stream)
                .toList();
    }

    private List<MethodSpec> autocompleteMethods(
            ClassManifest manifest,
            String fieldName,
            String repositoryName,
            int defaultLimit
    ) {
        var methodName = "autocompleteBy" + toPascalCase(fieldName);
        var returnType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        return List.of(
                MethodSpec.methodBuilder(methodName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "query")
                        .returns(returnType)
                        .addStatement("return $N(query, 0, $L)", methodName, defaultLimit)
                        .build(),
                MethodSpec.methodBuilder(methodName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "query")
                        .addParameter(int.class, "page")
                        .addParameter(int.class, "limit")
                        .returns(returnType)
                        .addStatement("log.debug($S, $T.class.getSimpleName(), $S)",
                                "Autocompleting {} by {}", manifest.className(), fieldName)
                        .addStatement("$T offset = (long) page * limit", long.class)
                        .addStatement("return $L",
                                repositoryCall(repositoryName, methodName))
                        .build());
    }

    private String repositoryCall(String repositoryName, String methodName) {
        if (JDBC_INCLUDED) {
            return repositoryName + "." + methodName + "(query, limit, offset)";
        }
        if (MONGO_INCLUDED) {
            return repositoryName + "." + methodName + "(query, "
                    + "org.springframework.data.domain.PageRequest.of(page, limit))";
        }
        return repositoryName + "." + methodName + "(query, "
                + "org.springframework.data.domain.PageRequest.of(page, limit))";
    }

    private static boolean isClassIncluded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

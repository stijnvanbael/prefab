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
                        .addStatement("return $N.$N(query, $T.of(page, limit))",
                                repositoryName,
                                methodName,
                                ClassName.get("org.springframework.data.domain", "PageRequest"))
                        .build());
    }
}

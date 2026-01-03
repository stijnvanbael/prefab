package be.appify.prefab.processor.getlist;

import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import org.atteo.evo.inflector.English;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

import static be.appify.prefab.processor.getlist.GetListUtil.filterPropertiesOf;
import static org.apache.commons.text.WordUtils.capitalize;
import static org.apache.commons.text.WordUtils.uncapitalize;

class GetListServiceWriter {
    MethodSpec getListMethod(ClassManifest manifest) {
        var method = MethodSpec.methodBuilder("getList")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Pageable.class, "pageable");
        manifest.parent().ifPresent(parent -> method.addParameter(String.class,
                uncapitalize(parent.name()) + "Id"));
        var filterProperties = filterPropertiesOf(manifest);
        filterProperties.forEach(filter ->
                method.addParameter(filter.field().type().asTypeName(), filter.field().name()));
        TypeName typeName = manifest.type().asTypeName();
        method.returns(ParameterizedTypeName.get(ClassName.get(Page.class), typeName));
        if (!filterProperties.isEmpty()) {
            logWithFilters(manifest, method, filterProperties);
        } else {
            method.addStatement("log.debug($S, $T.class.getSimpleName())", "Getting {}", manifest.className());
        }
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        if (filterProperties.isEmpty()) {
            method.addStatement("return $N.find$N($L)",
                    repositoryName,
                    manifest.parent().map(parent -> "By" + capitalize(parent.name())).orElse("All"),
                    manifest.parent().map(parent -> parent.name() + "Id, ").orElse("") + "pageable");
        } else {
            findWithFilters(manifest, method, repositoryName, filterProperties);
        }
        return method.build();
    }

    private void findWithFilters(
            ClassManifest manifest,
            MethodSpec.Builder method,
            String repositoryName,
            List<GetListUtil.FilterManifest> filters
    ) {
        method.addStatement("""
                        return $N.findAll(
                            $T.of(new $T($L),
                                $T.matchingAll()
                                    $L
                                    $L
                                    $L),
                                pageable)""",
                repositoryName,
                Example.class,
                manifest.className(),
                manifest.fields().stream()
                        .map(field -> filters.stream()
                                .anyMatch(property ->
                                        property.field().name().equals(field.name())) ? CodeBlock.of(field.name()) :
                                manifest.parent().filter(parent -> parent.name().equals(field.name()))
                                        .map(parent -> CodeBlock.of("referenceFactory.referenceTo($T.class, $NId)",
                                                parent.type().parameters().getFirst().asTypeName(),
                                                uncapitalize(parent.name())))
                                        .orElse(defaultValueForType(field.type())))
                        .collect(CodeBlock.joining(", ")),
                ExampleMatcher.class,
                CodeBlock.of(".withIgnorePaths($L)", manifest.fields().stream()
                        .filter(field -> isFiltered(manifest, filters, field))
                        .map(field -> "\"" + field.name() + "\"")
                        .collect(Collectors.joining(", "))),
                filters.stream()
                        .map(property -> CodeBlock.of(".withMatcher($S, $T.$N().$L())",
                                property.field().name(),
                                ExampleMatcher.GenericPropertyMatchers.class,
                                property.ignoreCase() ? "ignoreCase" : "caseSensitive",
                                matcherMethod(property.operator())))
                        .collect(CodeBlock.joining("\n")),
                manifest.parent().map(parent -> CodeBlock.of(".withMatcher($S, $T.caseSensitive().exact())",
                                parent.name(),
                                ExampleMatcher.GenericPropertyMatchers.class))
                        .orElse(CodeBlock.of(""))
        );
    }

    private String matcherMethod(Filter.Operator operator) {
        return switch (operator) {
            case EQUAL -> "exact";
            case CONTAINS -> "contains";
            case STARTS_WITH -> "startsWith";
            case ENDS_WITH -> "endsWith";
            case MATCHES_REGEX -> "regex";
        };
    }

    private static boolean isFiltered(
            ClassManifest manifest,
            List<GetListUtil.FilterManifest> filters,
            VariableManifest field
    ) {
        return filters.stream().noneMatch(filter -> filter.field().name().equals(field.name()))
                && manifest.parent().stream().noneMatch(parent -> parent.name().equals(field.name()));
    }

    private static void logWithFilters(
            ClassManifest manifest,
            MethodSpec.Builder method,
            List<GetListUtil.FilterManifest> filters
    ) {
        method.addStatement("log.debug($S, $T.class.getSimpleName(), $L)",
                "Getting %s by %s".formatted(
                        English.plural(manifest.simpleName()),
                        filters.stream()
                                .map(filter -> "%s: {}".formatted(filter.field().name()))
                                .collect(Collectors.joining(", "))),
                manifest.className(),
                filters.stream()
                        .map(filter -> filter.field().name())
                        .collect(Collectors.joining(", ")));
    }

    private CodeBlock defaultValueForType(TypeManifest type) {
        if (!type.isStandardType()) {
            return CodeBlock.of("null");
        }
        return switch (type.simpleName()) {
            case "boolean" -> CodeBlock.of("false");
            case "byte", "short", "int", "long", "float", "double", "char" -> CodeBlock.of("0");
            default -> CodeBlock.of("null");
        };
    }
}

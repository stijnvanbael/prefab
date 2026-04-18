package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.VariableManifest;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.atteo.evo.inflector.English;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static be.appify.prefab.processor.rest.getlist.GetListUtil.filterPropertiesOf;
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
                method.addParameter(filterParamType(filter.field()), filter.field().name()));
        TypeName typeName = manifest.type().asTypeName();
        method.returns(ParameterizedTypeName.get(ClassName.get(Page.class), typeName));
        var tenantField = manifest.tenantIdField();
        if (!filterProperties.isEmpty()) {
            if (tenantField.isEmpty()) {
                logWithFilters(manifest, method, filterProperties);
            } else {
                method.addStatement("log.debug($S, $T.class.getSimpleName())", "Getting {}",
                        manifest.className());
            }
        } else {
            method.addStatement("log.debug($S, $T.class.getSimpleName())", "Getting {}", manifest.className());
        }
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        if (tenantField.isPresent()) {
            findWithTenant(manifest, method, repositoryName, filterProperties, tenantField.get());
        } else if (filterProperties.isEmpty()) {
            method.addStatement("return $N.find$N($L)",
                    repositoryName,
                    manifest.parent().map(parent -> "By" + capitalize(parent.name())).orElse("All"),
                    manifest.parent().map(parent -> parent.name() + "Id, ").orElse("") + "pageable");
        } else {
            findWithFilters(manifest, method, repositoryName, filterProperties);
        }
        return method.build();
    }

    private void findWithTenant(
            ClassManifest manifest,
            MethodSpec.Builder method,
            String repositoryName,
            List<GetListUtil.FilterManifest> filters,
            VariableManifest tenantField
    ) {
        if (filters.isEmpty()) {
            findWithTenantOnly(manifest, method, repositoryName, tenantField);
        } else {
            findWithTenantAndFilters(manifest, method, repositoryName, filters, tenantField);
        }
    }

    private void findWithTenantOnly(
            ClassManifest manifest,
            MethodSpec.Builder method,
            String repositoryName,
            VariableManifest tenantField
    ) {
        method.addStatement("var tenantId = tenantContextProvider.currentTenantId()");
        method.addStatement("""
                        return tenantId != null
                            ? $N.findBy$N(tenantId, pageable)
                            : $N.find$N($L)""",
                repositoryName,
                capitalize(tenantField.name()),
                repositoryName,
                manifest.parent().map(parent -> "By" + capitalize(parent.name())).orElse("All"),
                manifest.parent().map(parent -> parent.name() + "Id, ").orElse("") + "pageable");
    }

    private void findWithTenantAndFilters(
            ClassManifest manifest,
            MethodSpec.Builder method,
            String repositoryName,
            List<GetListUtil.FilterManifest> filters,
            VariableManifest tenantField
    ) {
        method.addStatement("""
                        return $N.findAll(
                            $T.of(new $T($L),
                                $T.matchingAll()
                                    $L
                                    $L
                                    $L
                                    $L),
                                pageable)""",
                repositoryName,
                Example.class,
                manifest.className(),
                fieldsWithTenant(manifest, filters, tenantField),
                ExampleMatcher.class,
                ignorePathsWithTenant(manifest, filters, tenantField),
                filterMatchers(filters),
                tenantMatcher(tenantField),
                parentMatcher(manifest));
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
                fields(manifest, filters),
                ExampleMatcher.class,
                ignorePaths(manifest, filters),
                filterMatchers(filters),
                parentMatcher(manifest)
        );
    }

    private CodeBlock fields(ClassManifest manifest, List<GetListUtil.FilterManifest> filters) {
        return manifest.fields().stream()
                .map(field -> filters.stream()
                        .anyMatch(property -> property.field().name().equals(field.name()))
                        ? filterField(field)
                        : parentField(manifest, field))
                .collect(CodeBlock.joining(", "));
    }

    private CodeBlock fieldsWithTenant(
            ClassManifest manifest,
            List<GetListUtil.FilterManifest> filters,
            VariableManifest tenantField
    ) {
        return manifest.fields().stream()
                .map(field -> {
                    if (field.name().equals(tenantField.name())) {
                        return CodeBlock.of("tenantContextProvider.currentTenantId()");
                    }
                    if (filters.stream().anyMatch(f -> f.field().name().equals(field.name()))) {
                        return filterField(field);
                    }
                    return parentField(manifest, field);
                })
                .collect(CodeBlock.joining(", "));
    }

    private static CodeBlock filterField(VariableManifest field) {
        return field.type().isSingleValueType()
                ? CodeBlock.of("$N != null ? new $T($N) : null",
                field.name(), field.type().asTypeName(), field.name())
                : CodeBlock.of(field.name());
    }

    private static TypeName filterParamType(VariableManifest field) {
        return field.type().isSingleValueType()
                ? field.type().fields().getFirst().type().asBoxed().asTypeName()
                : field.type().asTypeName();
    }

    private CodeBlock parentField(ClassManifest manifest, VariableManifest field) {
        return manifest.parent().filter(parent -> parent.name().equals(field.name()))
                .map(parent -> CodeBlock.of("$NId != null ? new $T($NId) : null",
                        uncapitalize(parent.name()), parent.type().asTypeName(), uncapitalize(parent.name())))
                .orElse(defaultValueForType(field.type()));
    }

    private static CodeBlock ignorePaths(ClassManifest manifest, List<GetListUtil.FilterManifest> filters) {
        return ignorePathsExcluding(manifest, filters, null);
    }

    private static CodeBlock ignorePathsWithTenant(
            ClassManifest manifest,
            List<GetListUtil.FilterManifest> filters,
            VariableManifest tenantField
    ) {
        return ignorePathsExcluding(manifest, filters, tenantField.name());
    }

    private static CodeBlock ignorePathsExcluding(
            ClassManifest manifest,
            List<GetListUtil.FilterManifest> filters,
            String excludedFieldName
    ) {
        return CodeBlock.of(".withIgnorePaths($L)", manifest.fields().stream()
                .filter(field -> isFiltered(manifest, filters, field)
                        && !field.name().equals(excludedFieldName))
                .map(field -> "\"" + field.name() + "\"")
                .collect(Collectors.joining(", ")));
    }

    private CodeBlock filterMatchers(List<GetListUtil.FilterManifest> filters) {
        return filters.stream()
                .map(property -> CodeBlock.of(".withMatcher($S, $T.$N().$L())",
                        property.field().name(),
                        ExampleMatcher.GenericPropertyMatchers.class,
                        property.ignoreCase() ? "ignoreCase" : "caseSensitive",
                        matcherMethod(property.operator())))
                .collect(CodeBlock.joining("\n"));
    }

    private static CodeBlock tenantMatcher(VariableManifest tenantField) {
        return CodeBlock.of(".withMatcher($S, $T.caseSensitive().exact())",
                tenantField.name(),
                ExampleMatcher.GenericPropertyMatchers.class);
    }

    private static CodeBlock parentMatcher(ClassManifest manifest) {
        return manifest.parent().map(parent -> CodeBlock.of(".withMatcher($S, $T.caseSensitive().exact())",
                        parent.name(),
                        ExampleMatcher.GenericPropertyMatchers.class))
                .orElse(CodeBlock.of(""));
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
        var filterFieldNames = filters.stream()
                .map(filter -> filter.field().name())
                .toList();
        method.addStatement("log.debug($S, $T.class.getSimpleName(), $L)",
                "Getting %s by %s".formatted(
                        English.plural(manifest.simpleName()),
                        filterFieldNames.stream()
                                .map("%s: {}"::formatted)
                                .collect(Collectors.joining(", "))),
                manifest.className(),
                String.join(", ", filterFieldNames));
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

    MethodSpec getListMethod(PolymorphicAggregateManifest manifest) {
        TypeName typeName = manifest.type().asTypeName();
        var repositoryName = uncapitalize(manifest.simpleName()) + "Repository";
        return MethodSpec.methodBuilder("getList")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Pageable.class, "pageable")
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class), typeName))
                .addStatement("log.debug($S)", "Getting " + English.plural(manifest.simpleName()))
                .addStatement("return $N.findAll(pageable)", repositoryName)
                .build();
    }
}


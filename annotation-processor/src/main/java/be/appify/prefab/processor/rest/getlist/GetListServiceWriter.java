package be.appify.prefab.processor.rest.getlist;

import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.SortOrder;
import be.appify.prefab.core.spring.data.FullTextCondition;
import be.appify.prefab.core.spring.data.RangeCondition;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static be.appify.prefab.processor.rest.getlist.GetListUtil.exampleFiltersOf;
import static be.appify.prefab.processor.rest.getlist.GetListUtil.filterPropertiesOf;
import static be.appify.prefab.processor.rest.getlist.GetListUtil.fullTextFiltersOf;
import static be.appify.prefab.processor.rest.getlist.GetListUtil.rangeFiltersOf;
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
                method.addParameter(filterParamType(filter), filter.parameterName()));
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
        if (!rangeFiltersOf(filterProperties).isEmpty() || !fullTextFiltersOf(filterProperties).isEmpty()) {
            findWithExecutor(manifest, method, filterProperties, tenantField);
        } else if (tenantField.isPresent()) {
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

    private void findWithExecutor(
            ClassManifest manifest,
            MethodSpec.Builder method,
            List<GetListUtil.FilterManifest> filters,
            Optional<VariableManifest> tenantField
    ) {
        var exampleFilters = exampleFiltersOf(filters);
        var fields = tenantField
                .map(tenant -> fieldsWithTenant(manifest, exampleFilters, tenant))
                .orElseGet(() -> fields(manifest, exampleFilters));
        var ignorePaths = tenantField
                .map(tenant -> ignorePathsWithTenant(manifest, exampleFilters, tenant))
                .orElseGet(() -> ignorePaths(manifest, exampleFilters));
        var fullText = fullTextFiltersOf(filters).stream().findFirst();
        method.addStatement("""
                        return $N.findAll(
                            $T.of(new $T($L),
                                $T.matchingAll()
                                    $L
                                    $L
                                    $L
                                    $L),
                            $L,
                            pageable)""",
                fullText.isPresent() ? "fullTextQueryExecutor" : "rangeQueryExecutor",
                Example.class,
                manifest.className(),
                fields,
                ExampleMatcher.class,
                ignorePaths,
                filterMatchers(exampleFilters),
                tenantField.map(GetListServiceWriter::tenantMatcher).orElse(CodeBlock.of("")),
                parentMatcher(manifest),
                fullText.map(filter -> CodeBlock.of("$L, $L", fullTextCondition(filter),
                                rangeConditions(rangeFiltersOf(filters))))
                        .orElseGet(() -> rangeConditions(rangeFiltersOf(filters))));
    }

    private static CodeBlock fullTextCondition(GetListUtil.FilterManifest filter) {
        return CodeBlock.of("new $T($T.of($L), $N)",
                FullTextCondition.class,
                List.class,
                filter.searchedProperties().stream().map(property -> CodeBlock.of("$S", property))
                        .collect(CodeBlock.joining(", ")),
                filter.parameterName());
    }

    private static CodeBlock rangeConditions(List<GetListUtil.FilterManifest> ranges) {
        return CodeBlock.of("$T.of($L)", List.class, ranges.stream()
                .map(range -> CodeBlock.of("new $T($S, $T.$N, $N)",
                        RangeCondition.class,
                        range.field().name(),
                        Filter.Operator.class,
                        range.operator().name(),
                        range.parameterName()))
                .collect(CodeBlock.joining(", ")));
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

    MethodSpec getListByQueryMethod(ClassManifest manifest, GetList getList) {
        var queryType = GetListQueryWriter.queryType(manifest);
        var method = MethodSpec.methodBuilder("getList")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(queryType, "query")
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class), manifest.type().asTypeName()));
        method.addStatement("var pageable = $T.of(query.page(), query.pageSize(), query.sort().orElse($L))",
                PageRequest.class, defaultSort(getList));
        var arguments = Stream.concat(
                        manifest.parent().stream().map(parent -> CodeBlock.of(
                                "query.$N().orElseThrow(() -> new $T($S))",
                                GetListQueryWriter.parentIdName(manifest),
                                IllegalArgumentException.class,
                                "%s is required".formatted(GetListQueryWriter.parentIdName(manifest)))),
                        filterPropertiesOf(manifest).stream()
                                .map(filter -> CodeBlock.of("query.$N().orElse(null)", filter.parameterName())))
                .collect(CodeBlock.joining(", "));
        method.addStatement(arguments.isEmpty()
                ? CodeBlock.of("return getList(pageable)")
                : CodeBlock.of("return getList(pageable, $L)", arguments));
        return method.build();
    }

    private static CodeBlock defaultSort(GetList getList) {
        if (getList.sorted().length == 0) {
            return CodeBlock.of("$T.unsorted()", Sort.class);
        }
        return CodeBlock.of("$T.by($L)", Sort.class, Stream.of(getList.sorted())
                .map(order -> CodeBlock.of("new $T($T.$L, $S)",
                        Sort.Order.class,
                        Sort.Direction.class,
                        order.direction() == SortOrder.Direction.ASCENDING ? "ASC" : "DESC",
                        order.property()))
                .collect(CodeBlock.joining(", ")));
    }

    static TypeName filterParamType(GetListUtil.FilterManifest filter) {
        var field = filter.field();
        if (filter.isRange()) {
            return field.type().asBoxed().asTypeName();
        }
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
            case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, FULL_TEXT ->
                    throw new IllegalArgumentException("Operator %s has no example matcher".formatted(operator));
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
                .map(GetListUtil.FilterManifest::parameterName)
                .toList();
        method.addStatement("log.debug($S, $T.class.getSimpleName(), $L)",
                "Getting %s by %s".formatted(
                        manifest.plural(),
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
        var method = MethodSpec.methodBuilder("getList")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Page.class), typeName));
        manifest.parent().ifPresent(parent ->
                method.addParameter(String.class, uncapitalize(parent.name()) + "Id"));
        method.addParameter(Pageable.class, "pageable");
        method.addStatement("log.debug($S)", "Getting " + manifest.plural());
        manifest.parent().ifPresentOrElse(
                parent -> method.addStatement("return $N.findBy$N($NId, pageable)",
                        repositoryName, capitalize(parent.name()), uncapitalize(parent.name())),
                () -> method.addStatement("return $N.findAll(pageable)", repositoryName));
        return method.build();
    }
}


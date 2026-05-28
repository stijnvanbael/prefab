package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.processor.ClassManifest;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import javax.lang.model.element.Modifier;
import java.util.List;

import static be.appify.prefab.processor.CaseUtil.toPascalCase;
import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

class AutocompleteRepositoryWriter {

    private static final boolean JDBC_INCLUDED = isClassIncluded("org.springframework.data.relational.core.mapping.Table");
    private static final boolean MONGO_INCLUDED = isClassIncluded("org.springframework.data.mongodb.core.MongoTemplate");

    List<MethodSpec> autocompleteMethods(ClassManifest manifest) {
        var tableName = toSnakeCase(manifest.simpleName());
        return manifest.fields().stream()
                .map(field -> field.getAnnotation(Autocomplete.class)
                        .map(annotation -> autocompleteMethod(
                                field.name(),
                                tableName,
                                annotation.value().ignoreCase())))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    static MethodSpec autocompleteJdbcMethod(String fieldName, String tableName, boolean ignoreCase) {
        var methodName = methodNameFor(fieldName);
        var columnName = quoted(toSnakeCase(fieldName));
        var table = quoted(tableName);
        var whereClause = ignoreCase
                ? "LOWER(" + columnName + ") LIKE LOWER(CONCAT('%', :query, '%'))"
                : columnName + " LIKE CONCAT('%', :query, '%')";
        var sql = "SELECT DISTINCT " + columnName + " FROM " + table + " WHERE " + whereClause
                + " ORDER BY " + columnName;

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(
                                ClassName.get("org.springframework.data.jdbc.repository.query", "Query"))
                        .addMember("value", "$S", sql)
                        .build())
                .addParameter(com.palantir.javapoet.ParameterSpec.builder(String.class, "query")
                        .addAnnotation(AnnotationSpec.builder(Param.class)
                                .addMember("value", "$S", "query")
                                .build())
                        .build())
                .addParameter(Pageable.class, "pageable")
                .returns(ParameterizedTypeName.get(List.class, String.class))
                .build();
    }

    static MethodSpec autocompleteMongoMethod(String fieldName, boolean ignoreCase) {
        var methodName = methodNameFor(fieldName);
        var matchStage = ignoreCase
                ? "{ '$match': { '" + fieldName + "': { '$regex': '?0', '$options': 'i' } } }"
                : "{ '$match': { '" + fieldName + "': { '$regex': '?0' } } }";
        var groupStage = "{ '$group': { '_id': '$" + fieldName + "' } }";
        var sortStage = "{ '$sort': { '_id': 1 } }";

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(
                                ClassName.get("org.springframework.data.mongodb.repository", "Aggregation"))
                        .addMember("pipeline", "{$S, $S, $S}", matchStage, groupStage, sortStage)
                        .build())
                .addParameter(String.class, "query")
                .addParameter(Pageable.class, "pageable")
                .returns(ParameterizedTypeName.get(List.class, String.class))
                .build();
    }

    private MethodSpec autocompleteMethod(String fieldName, String tableName, boolean ignoreCase) {
        if (JDBC_INCLUDED) {
            return autocompleteJdbcMethod(fieldName, tableName, ignoreCase);
        }
        if (MONGO_INCLUDED) {
            return autocompleteMongoMethod(fieldName, ignoreCase);
        }
        return MethodSpec.methodBuilder(methodNameFor(fieldName))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(String.class, "query")
                .addParameter(Pageable.class, "pageable")
                .returns(ParameterizedTypeName.get(List.class, String.class))
                .build();
    }

    private static String methodNameFor(String fieldName) {
        return "autocompleteBy" + toPascalCase(fieldName);
    }

    private static String quoted(String value) {
        return "\"" + value + "\"";
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


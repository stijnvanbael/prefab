package be.appify.prefab.processor.rest.autocomplete;

import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.core.annotations.rest.MatchStrategy;
import be.appify.prefab.core.annotations.rest.ScanMode;
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
                                annotation.value().scanMode(),
                                annotation.value().matchStrategy())))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    static MethodSpec autocompleteJdbcMethod(String fieldName, String tableName, ScanMode scanMode, MatchStrategy matchStrategy) {
        var methodName = methodNameFor(fieldName);
        var columnName = quoted(toSnakeCase(fieldName));
        var table = quoted(tableName);
        var whereClause = buildJdbcWhereClause(columnName, scanMode, matchStrategy);
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

    static MethodSpec autocompleteMongoMethod(String fieldName, ScanMode scanMode, MatchStrategy matchStrategy) {
        var methodName = methodNameFor(fieldName);
        var matchStage = buildMongoMatchStage(fieldName, scanMode, matchStrategy);
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

    // --- query shape builders ---

    private static String buildJdbcWhereClause(String columnName, ScanMode scanMode, MatchStrategy matchStrategy) {
        return switch (matchStrategy) {
            case EXACT -> columnName + " LIKE " + concatJdbc(scanMode);
            case IGNORE_CASE -> "LOWER(" + columnName + ") LIKE LOWER(" + concatJdbc(scanMode) + ")";
            // FUZZY: uses pg_trgm similarity; falls back to PREFIX/CONTAINS with case-insensitive for portability
            case FUZZY -> "similarity(" + columnName + ", :query) > 0.3"
                    + " OR LOWER(" + columnName + ") LIKE LOWER(" + concatJdbc(scanMode) + ")";
        };
    }

    private static String concatJdbc(ScanMode scanMode) {
        return switch (scanMode) {
            case PREFIX -> "CONCAT(:query, '%')";
            case CONTAINS -> "CONCAT('%', :query, '%')";
        };
    }

    private static String buildMongoMatchStage(String fieldName, ScanMode scanMode, MatchStrategy matchStrategy) {
        var pattern = switch (scanMode) {
            case PREFIX -> "^?0";
            case CONTAINS -> "?0";
        };
        // MongoDB has no native fuzzy support; FUZZY falls back to case-insensitive regex
        var options = (matchStrategy == MatchStrategy.EXACT) ? "" : ", '$options': 'i'";
        return "{ '$match': { '" + fieldName + "': { '$regex': '" + pattern + "'" + options + " } } }";
    }

    // --- delegation ---

    private MethodSpec autocompleteMethod(String fieldName, String tableName, ScanMode scanMode, MatchStrategy matchStrategy) {
        if (JDBC_INCLUDED) {
            return autocompleteJdbcMethod(fieldName, tableName, scanMode, matchStrategy);
        }
        if (MONGO_INCLUDED) {
            return autocompleteMongoMethod(fieldName, scanMode, matchStrategy);
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


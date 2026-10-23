package be.appify.prefab.processor.mongodb;

import be.appify.prefab.core.annotations.Indexed;
import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.Filters;
import be.appify.prefab.core.util.FullTextLanguages;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.VariableManifest;
import be.appify.prefab.processor.rest.getlist.FullTextFields;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static be.appify.prefab.processor.CaseUtil.toSnakeCase;

class MongoIndexWriter {
    private static final ClassName MONGO_TEMPLATE =
            ClassName.get("org.springframework.data.mongodb.core", "MongoTemplate");
    private static final ClassName INDEX =
            ClassName.get("org.springframework.data.mongodb.core.index", "Index");
    private static final ClassName TEXT_INDEX = ClassName.get(
            "org.springframework.data.mongodb.core.index", "TextIndexDefinition");
    private static final ClassName MONGO_TEXT_INDEXES = ClassName.get(
            "be.appify.prefab.mongodb.spring.data.mongodb", "MongoTextIndexes");
    private static final ClassName SORT_DIRECTION =
            ClassName.get("org.springframework.data.domain.Sort", "Direction");

    private final PrefabContext context;

    MongoIndexWriter(PrefabContext context) {
        this.context = context;
    }

    void write(List<ClassManifest> manifests) {
        var fileWriter = new OutputTargetFileOutput(context, "infrastructure.mongodb", OutputTarget.MAIN);
        var rootPackage = findCommonRootPackage(manifests);

        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(MONGO_TEMPLATE, "mongoTemplate");
        if (manifests.stream().anyMatch(manifest -> !FullTextFields.weightsOf(manifest).isEmpty())) {
            constructor.addParameter(ParameterSpec.builder(String.class, "fullTextLanguage")
                    .addAnnotation(AnnotationSpec.builder(Value.class)
                            .addMember("value", "$S", "${%s:%s}".formatted(
                                    FullTextLanguages.PROPERTY, FullTextLanguages.NEUTRAL))
                            .build())
                    .build());
        }

        for (ClassManifest manifest : manifests) {
            fieldIndexesOf(manifest, null).forEach(index ->
                    constructor.addStatement(
                            "mongoTemplate.indexOps($T.class).createIndex(new $T($S, $T.ASC)$L)",
                            manifest.type().asTypeName(),
                            INDEX,
                            index.fieldPath(),
                            SORT_DIRECTION,
                            index.unique() ? ".unique()" : ""
                    )
            );
            textIndexOf(constructor, manifest);
        }

        var type = TypeSpec.classBuilder("MongoIndexConfiguration")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Configuration.class)
                .addMethod(constructor.build())
                .build();

        fileWriter.writeFile(rootPackage, "MongoIndexConfiguration", type);
    }

    // MongoDB allows one text index per collection, which is why an aggregate has at most one FULL_TEXT parameter.
    // The language defaults to the application-wide property and is "none" (no stemming) for the PostgreSQL 'simple'
    // configuration. A language override field that no document has prevents a property called "language" from
    // being read as a text language. A changed language rebuilds the index.
    private static void textIndexOf(MethodSpec.Builder constructor, ClassManifest manifest) {
        var weights = FullTextFields.weightsOf(manifest);
        if (weights.isEmpty()) {
            return;
        }
        var fields = weights.entrySet().stream().allMatch(entry -> entry.getValue() == Filter.Weight.D)
                ? ".onFields(" + weights.keySet().stream().map(field -> "\"" + field + "\"")
                .collect(Collectors.joining(", ")) + ")"
                : weights.entrySet().stream()
                        .map(entry -> ".onField(\"%s\", %sf)".formatted(entry.getKey(),
                                entry.getValue().textIndexWeight()))
                        .collect(Collectors.joining("\n"));
        var language = CodeBlock.of("$T.mongoNameOf($L)", FullTextLanguages.class,
                FullTextFields.languageOf(manifest).map(name -> CodeBlock.of("$S", name))
                        .orElse(CodeBlock.of("fullTextLanguage")));
        constructor.addStatement("""
                        $T.createOrRebuild(mongoTemplate.indexOps($T.class), $T.builder()
                            $L
                            .withDefaultLanguage($L)
                            .withLanguageOverride("_prefabLanguage")
                            .build(), $L)""",
                MONGO_TEXT_INDEXES,
                manifest.type().asTypeName(),
                TEXT_INDEX,
                fields,
                language,
                language);
    }

    private List<MongoIndex> fieldIndexesOf(ClassManifest manifest, String prefix) {
        var indexes = new ArrayList<MongoIndex>();
        for (var field : manifest.fields()) {
            if (field.type().isRecord() && !field.type().isSingleValueType()) {
                var newPrefix = prefix != null ? prefix + "." + field.name() : field.name();
                indexes.addAll(fieldIndexesOf(field.type().asClassManifest(), newPrefix));
            } else {
                var fieldPath = prefix != null ? prefix + "." + field.name() : field.name();
                indexFor(fieldPath, field).ifPresent(indexes::add);
            }
        }
        return indexes;
    }

    private static java.util.Optional<MongoIndex> indexFor(String fieldPath, VariableManifest field) {
        if (field.hasAnnotation(Indexed.class)) {
            var unique = field.getAnnotation(Indexed.class)
                    .map(a -> a.value().unique())
                    .orElse(false);
            return java.util.Optional.of(new MongoIndex(fieldPath, unique));
        } else if (FullTextFields.isOnlyFullText(field)) {
            return java.util.Optional.empty();
        } else if (field.hasAnnotation(Filter.class) || field.hasAnnotation(Filters.class)) {
            return java.util.Optional.of(new MongoIndex(fieldPath, false));
        }
        return java.util.Optional.empty();
    }

    private static String findCommonRootPackage(List<ClassManifest> manifests) {
        return manifests.stream()
                .map(ClassManifest::packageName)
                .reduce((pkg1, pkg2) -> {
                    var parts1 = pkg1.split("\\.");
                    var parts2 = pkg2.split("\\.");
                    var minLength = Math.min(parts1.length, parts2.length);
                    var sb = new StringBuilder();
                    for (int i = 0; i < minLength; i++) {
                        if (parts1[i].equals(parts2[i])) {
                            if (!sb.isEmpty()) sb.append(".");
                            sb.append(parts1[i]);
                        } else {
                            break;
                        }
                    }
                    return sb.toString();
                })
                .orElseThrow();
    }

    record MongoIndex(String fieldPath, boolean unique) {
    }
}

package be.appify.prefab.avro.processor;

import org.apache.avro.Schema;
import org.apache.avro.SchemaNormalization;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cross-AVSC named-type registry for a single annotation-processing round.
 *
 * <p>Detects binary incompatibilities (ERROR) and doc/sample divergence (WARNING) when the
 * same fully-qualified Avro name appears in more than one AVSC file.
 */
class AvscRecordRegistry {

    private record Entry(String path, Schema schema, Element element) {}

    private final Messager messager;
    private final Map<String, Entry> registry = new LinkedHashMap<>();

    AvscRecordRegistry(ProcessingEnvironment processingEnvironment) {
        this.messager = processingEnvironment.getMessager();
    }

    /**
     * Recursively collects all named types from {@code rootSchema} and registers them against
     * {@code path}. Emits an ERROR if a name clashes with a binary-incompatible existing
     * definition, or a WARNING when the schemas are binary-identical but doc or sample values
     * diverge.
     */
    void registerAll(String path, Schema rootSchema, Element element) {
        var namedTypes = new LinkedHashMap<String, Schema>();
        collectNamedTypesInto(rootSchema, namedTypes);
        namedTypes.forEach((fqName, schema) -> register(fqName, path, schema, element));
    }

    private void register(String fqName, String path, Schema candidate, Element element) {
        var existing = registry.get(fqName);
        if (existing == null) {
            registry.put(fqName, new Entry(path, candidate, element));
            return;
        }
        checkCompatibility(fqName, existing, path, candidate, element);
    }

    private void checkCompatibility(
            String fqName, Entry existing, String candidatePath, Schema candidate, Element element) {
        var existingPcf = SchemaNormalization.toParsingForm(existing.schema());
        var candidatePcf = SchemaNormalization.toParsingForm(candidate);
        if (!existingPcf.equals(candidatePcf)) {
            messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Record " + fqName + " in " + candidatePath
                            + " is not binary-compatible with definition in " + existing.path() + ".",
                    element);
            return;
        }
        checkDocAndSampleDivergence(fqName, existing, candidatePath, candidate, element);
    }

    private void checkDocAndSampleDivergence(
            String fqName, Entry existing, String candidatePath, Schema candidate, Element element) {
        checkSchemaDoc(fqName, existing.path(), existing.schema(), candidatePath, candidate, element);
        if (candidate.getType() == Schema.Type.RECORD) {
            for (var candidateField : candidate.getFields()) {
                var existingField = existing.schema().getField(candidateField.name());
                if (existingField == null) continue;
                checkFieldDoc(fqName, candidateField.name(), existingField, existing.path(), candidateField, candidatePath, element);
                checkFieldSample(fqName, candidateField.name(), existingField, existing.path(), candidateField, candidatePath, element);
            }
        }
    }

    private void checkSchemaDoc(
            String fqName, String pathA, Schema schemaA, String pathB, Schema schemaB, Element element) {
        var docA = nullToEmpty(schemaA.getDoc());
        var docB = nullToEmpty(schemaB.getDoc());
        if (!docA.equals(docB)) {
            messager.printMessage(
                    Diagnostic.Kind.WARNING,
                    "Record " + fqName + ": schema doc differs between " + pathA + " and " + pathB + ".",
                    element);
        }
    }

    private void checkFieldDoc(
            String fqName, String fieldName,
            Schema.Field existingField, String pathA,
            Schema.Field candidateField, String pathB,
            Element element) {
        var docA = nullToEmpty(existingField.doc());
        var docB = nullToEmpty(candidateField.doc());
        if (!docA.equals(docB)) {
            messager.printMessage(
                    Diagnostic.Kind.WARNING,
                    "Record " + fqName + ": doc on field " + fieldName
                            + " differs between " + pathA + " and " + pathB + ".",
                    element);
        }
    }

    private void checkFieldSample(
            String fqName, String fieldName,
            Schema.Field existingField, String pathA,
            Schema.Field candidateField, String pathB,
            Element element) {
        var sampleA = nullToEmpty(existingField.getProp("sample"));
        var sampleB = nullToEmpty(candidateField.getProp("sample"));
        if (!sampleA.equals(sampleB)) {
            messager.printMessage(
                    Diagnostic.Kind.WARNING,
                    "Record " + fqName + ": sample on field " + fieldName
                            + " differs between " + pathA + " and " + pathB + ".",
                    element);
        }
    }

    private void collectNamedTypesInto(Schema schema, Map<String, Schema> collected) {
        if (schema == null) return;
        switch (schema.getType()) {
            case RECORD -> {
                if (collected.containsKey(schema.getFullName())) return;
                collected.put(schema.getFullName(), schema);
                schema.getFields().forEach(field -> collectNamedTypesInto(field.schema(), collected));
            }
            case ENUM -> collected.putIfAbsent(schema.getFullName(), schema);
            case ARRAY -> collectNamedTypesInto(schema.getElementType(), collected);
            case UNION -> schema.getTypes().forEach(t -> collectNamedTypesInto(t, collected));
            default -> { /* primitives and logical types need no traversal */ }
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}


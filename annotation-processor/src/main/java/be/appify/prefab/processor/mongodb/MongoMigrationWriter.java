package be.appify.prefab.processor.mongodb;

import be.appify.prefab.core.annotations.DbRename;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.TypeManifest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

class MongoMigrationWriter {

    void writeMongoMigration(ProcessingEnvironment processingEnvironment, List<ClassManifest> classManifests) {
        var state = currentState(processingEnvironment);
        var desired = desiredChanges(classManifests);
        var newChanges = desired.stream()
                .filter(c -> !state.applied().contains(c))
                .toList();
        if (!newChanges.isEmpty()) {
            writeFile(processingEnvironment, classManifests, newChanges, state.version());
        }
    }

    private MigrationState currentState(ProcessingEnvironment processingEnvironment) {
        var applied = new HashSet<MongoChange>();
        var version = 0;
        for (int i = 1; ; i++) {
            try {
                var file = processingEnvironment.getFiler().getResource(
                        StandardLocation.CLASS_PATH, "mongo.migration",
                        "V%d__generated.js".formatted(i));
                try (var input = file.openInputStream()) {
                    var content = new String(input.readAllBytes());
                    applied.addAll(parseChanges(content));
                }
                version = i;
            } catch (FileNotFoundException e) {
                break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new MigrationState(applied, version);
    }

    private static final Pattern COLL_RENAME_PATTERN = Pattern.compile(
            "db\\.getCollection\\(\"([^\"]+)\"\\)\\.renameCollection\\(\"([^\"]+)\"\\)");

    private static final Pattern FIELD_RENAME_PATTERN = Pattern.compile(
            "db\\.getCollection\\(\"([^\"]+)\"\\)\\.updateMany\\(\\{},\\s*\\{\\s*\\$rename:\\s*\\{\\s*\"([^\"]+)\":\\s*\"([^\"]+)\"\\s*\\}\\s*\\}\\)");

    static Set<MongoChange> parseChanges(String content) {
        var changes = new HashSet<MongoChange>();
        var collMatch = COLL_RENAME_PATTERN.matcher(content);
        while (collMatch.find()) {
            changes.add(new MongoChange.RenameCollection(collMatch.group(1), collMatch.group(2)));
        }
        var fieldMatch = FIELD_RENAME_PATTERN.matcher(content);
        while (fieldMatch.find()) {
            changes.add(new MongoChange.RenameField(fieldMatch.group(1), fieldMatch.group(2), fieldMatch.group(3)));
        }
        return changes;
    }

    List<MongoChange> desiredChanges(List<ClassManifest> classManifests) {
        var changes = new ArrayList<MongoChange>();
        for (var manifest : classManifests) {
            var collectionName = collectionNameOf(manifest.type());
            manifest.annotationsOfType(DbRename.class).stream().findFirst().ifPresent(rename -> {
                var oldName = mongoCollectionNameFrom(rename.value());
                changes.add(new MongoChange.RenameCollection(oldName, collectionName));
            });
            changes.addAll(fieldRenamesOf(manifest, collectionName, null));
        }
        return changes;
    }

    private List<MongoChange> fieldRenamesOf(ClassManifest manifest, String collection, String prefix) {
        return manifest.fields().stream()
                .flatMap(field -> {
                    if (field.type().isRecord() && !field.type().isSingleValueType()) {
                        // Nested record: recurse with dot-path prefix
                        var newPrefix = prefix != null ? prefix + "." + field.name() : field.name();
                        return fieldRenamesOf(field.type().asClassManifest(), collection, newPrefix).stream();
                    } else {
                        // Leaf field (primitives, single-value types, Lists, etc.)
                        return field.getAnnotation(DbRename.class)
                                .map(ann -> {
                                    var oldValue = ann.value().value();
                                    var oldPath = prefix != null ? prefix + "." + oldValue : oldValue;
                                    var newPath = prefix != null ? prefix + "." + field.name() : field.name();
                                    return (MongoChange) new MongoChange.RenameField(collection, oldPath, newPath);
                                })
                                .stream();
                    }
                })
                .toList();
    }

    private static String collectionNameOf(TypeManifest manifest) {
        var name = manifest.simpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    static String mongoCollectionNameFrom(String javaStyleName) {
        if (javaStyleName == null || javaStyleName.isEmpty()) {
            return javaStyleName;
        }
        return Character.toLowerCase(javaStyleName.charAt(0)) + javaStyleName.substring(1);
    }

    private void writeFile(
            ProcessingEnvironment processingEnvironment,
            List<ClassManifest> classManifests,
            List<MongoChange> changes,
            int version
    ) {
        try {
            var resource = processingEnvironment.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "mongo/migration/V%d__generated.js".formatted(version + 1),
                    classManifests.stream().map(m -> m.type().asElement()).toArray(Element[]::new)
            );
            try (var writer = resource.openWriter()) {
                // Write collection renames before field renames (order matters)
                for (var change : changes) {
                    if (change instanceof MongoChange.RenameCollection) {
                        writer.write(change.toScript());
                    }
                }
                for (var change : changes) {
                    if (change instanceof MongoChange.RenameField) {
                        writer.write(change.toScript());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    record MigrationState(Set<MongoChange> applied, int version) {
    }
}

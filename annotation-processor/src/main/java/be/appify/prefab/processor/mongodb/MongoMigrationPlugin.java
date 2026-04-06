package be.appify.prefab.processor.mongodb;

import be.appify.prefab.core.annotations.MongoMigration;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;

import java.util.List;

/**
 * Prefab plugin that generates MongoDB migration scripts based on the {@link MongoMigration} annotation.
 * <p>
 * When an aggregate root is annotated with {@code @MongoMigration} and one or more of its fields (or the class
 * itself) carry {@code @DbRename}, this plugin generates a versioned JavaScript migration file under
 * {@code mongo/migration/V{N}__generated.js} that contains the corresponding
 * {@code renameCollection} / {@code $rename} commands.
 * </p>
 */
public class MongoMigrationPlugin implements PrefabPlugin {

    private final MongoMigrationWriter writer = new MongoMigrationWriter();
    private PrefabContext context;

    /** Creates a new instance of MongoMigrationPlugin. */
    public MongoMigrationPlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        var mongoManifests = manifests.stream()
                .filter(m -> !m.annotationsOfType(MongoMigration.class).isEmpty())
                .toList();
        if (!mongoManifests.isEmpty()) {
            writer.writeMongoMigration(context.processingEnvironment(), mongoManifests);
        }
    }
}

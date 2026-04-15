package be.appify.prefab.processor.mongodb;

import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.MongoMigration;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;

import java.util.List;

/**
 * Prefab plugin that generates MongoDB migration scripts based on the {@link DbMigration} annotation
 * (or the deprecated {@link MongoMigration} annotation).
 * <p>
 * When an aggregate root is annotated with {@code @DbMigration} (or the legacy {@code @MongoMigration})
 * and one or more of its fields (or the class itself) carry {@code @DbRename}, this plugin generates a
 * versioned JavaScript migration file under {@code mongo/migration/V{N}__generated.js} that contains the
 * corresponding {@code renameCollection} / {@code $rename} commands.
 * </p>
 * <p>
 * This plugin is only active when {@code MongoTemplate} is present on the compilation classpath,
 * i.e. when {@code spring-boot-starter-data-mongodb} (or equivalent) is a project dependency.
 * </p>
 */
public class MongoMigrationPlugin implements PrefabPlugin {

    private static final boolean MONGO_INCLUDED = isMongoIncluded();

    private final MongoMigrationWriter writer = new MongoMigrationWriter();
    private PrefabContext context;

    /** Creates a new instance of MongoMigrationPlugin. */
    public MongoMigrationPlugin() {
    }

    private static boolean isMongoIncluded() {
        try {
            Class.forName("org.springframework.data.mongodb.core.MongoTemplate");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        if (!MONGO_INCLUDED) {
            return;
        }
        var mongoManifests = manifests.stream()
                .filter(m -> !m.annotationsOfType(DbMigration.class).isEmpty()
                        || !m.annotationsOfType(MongoMigration.class).isEmpty())
                .toList();
        if (!mongoManifests.isEmpty()) {
            writer.writeMongoMigration(context.processingEnvironment(), mongoManifests);
        }
    }
}

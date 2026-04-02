package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import java.util.List;

/**
 * Prefab plugin that generates database migration scripts based on the @DbMigration annotation.
 */
public class DbMigrationPlugin implements PrefabPlugin {

    private final DbMigrationWriter dbMigrationWriter = new DbMigrationWriter();
    private PrefabContext context;

    /** Creates a new instance of DbMigrationPlugin. */
    public DbMigrationPlugin() {
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, List<PolymorphicAggregateManifest> polymorphicManifests) {
        var classManifests = manifests.stream()
                .filter(manifest -> !manifest.annotationsOfType(DbMigration.class).isEmpty())
                .toList();
        var polyManifests = polymorphicManifests.stream()
                .filter(PolymorphicAggregateManifest::hasDbMigration)
                .toList();
        if (!classManifests.isEmpty() || !polyManifests.isEmpty()) {
            dbMigrationWriter.writeDbMigration(context.processingEnvironment(), classManifests, polyManifests);
        }
    }
}

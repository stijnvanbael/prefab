package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;

import java.util.List;

public class DbMigrationPlugin implements PrefabPlugin {

    private final DbMigrationWriter dbMigrationWriter = new DbMigrationWriter();

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, PrefabContext prefabContext) {
        var classManifests = manifests.stream()
                .filter(manifest -> !manifest.annotationsOfType(DbMigration.class).isEmpty())
                .toList();
        if (!classManifests.isEmpty()) {
            dbMigrationWriter.writeDbMigration(prefabContext.processingEnvironment(), classManifests);
        }
    }
}

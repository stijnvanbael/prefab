package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class DbMigrationPlugin implements PrefabPlugin {

    private final DbMigrationWriter dbMigrationWriter = new DbMigrationWriter();

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests, PrefabContext prefabContext) {
        manifests.stream()
                .filter(manifest -> !manifest.annotationsOfType(DbMigration.class).isEmpty())
                .collect(Collectors.groupingBy(manifest ->
                        manifest.annotationsOfType(DbMigration.class).stream().findFirst().orElseThrow().version()))
                .forEach(dbMigrationWriter::writeDbMigration);
    }
}

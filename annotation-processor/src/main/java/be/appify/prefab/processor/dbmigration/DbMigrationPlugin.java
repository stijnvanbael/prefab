package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import java.util.List;

/**
 * Prefab plugin that generates database migration scripts based on the @DbMigration annotation.
 * <p>
 * This plugin is only active when Spring Data Relational is present on the compilation classpath,
 * i.e. when {@code spring-boot-starter-data-jdbc} (or equivalent) is a project dependency.
 * For MongoDB projects, {@code MongoMigrationPlugin} handles {@code @DbMigration} instead.
 * </p>
 */
public class DbMigrationPlugin implements PrefabPlugin {

    private static final boolean JDBC_INCLUDED = isJdbcIncluded();

    private DbMigrationWriter dbMigrationWriter;
    private PrefabContext context;

    /** Creates a new instance of DbMigrationPlugin. */
    public DbMigrationPlugin() {
    }

    private static boolean isJdbcIncluded() {
        try {
            Class.forName("org.springframework.data.relational.core.mapping.Table");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;
        this.dbMigrationWriter = new DbMigrationWriter(context);
    }

    @Override
    public void writeGlobalFiles(List<ClassManifest> manifests, List<PolymorphicAggregateManifest> polymorphicManifests) {
        if (!JDBC_INCLUDED) {
            return;
        }
        var classManifests = manifests.stream()
                .filter(this::isDbMigrationEnabled)
                .toList();
        var polyManifests = polymorphicManifests.stream()
                .filter(PolymorphicAggregateManifest::isDbMigrationEnabled)
                .toList();
        if (!classManifests.isEmpty() || !polyManifests.isEmpty()) {
            dbMigrationWriter.writeDbMigration(context.processingEnvironment(), classManifests, polyManifests);
        }
    }

    private boolean isDbMigrationEnabled(ClassManifest manifest) {
        var annotations = manifest.annotationsOfType(DbMigration.class);
        return annotations.stream().allMatch(DbMigration::enabled);
    }
}

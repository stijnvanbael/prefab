package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PolymorphicAggregateManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import be.appify.prefab.processor.SourceContributions;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;

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
    private DbColumnConverterContributorWriter converterContributorWriter;
    private PrefabContext context;
    private final SourceContributions<MigrationInput> inputs = new SourceContributions<>(MigrationInput.class);

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
        this.converterContributorWriter = new DbColumnConverterContributorWriter(context);
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
        converterContributorWriter.writeContributors(manifests);
        classManifests.forEach(m -> inputs.add(m.type().asElement(), dbMigrationWriter.input(m)));
        polyManifests.forEach(m -> inputs.add(m.type().asElement(), dbMigrationWriter.input(m)));
    }

    @Override
    public void finishProcessing() {
        if (!JDBC_INCLUDED) {
            return;
        }
        var merged = inputs.merge(context);
        if (merged.isEmpty()) {
            return;
        }
        var schema = merged.values().stream().flatMap(List::stream).toList();
        var names = schema.stream().map(MigrationInput::name).collect(Collectors.toSet());
        var manifestFile = new AggregateManifestFile(context.processingEnvironment());
        if (!manifestFile.isComplete(names)) {
            return;
        }
        var owners = merged.keySet().stream().map(context.processingEnvironment().getElementUtils()::getTypeElement)
                .filter(Objects::nonNull).toArray(Element[]::new);
        var check = ManualMigrationCheck.scan(context);
        check.check(dbMigrationWriter.writeDbMigration(schema, owners),
                names.stream().map(name -> name.substring(name.lastIndexOf('.') + 1)).toList());
        manifestFile.save(names, owners);
    }

    private boolean isDbMigrationEnabled(ClassManifest manifest) {
        var annotations = manifest.annotationsOfType(DbMigration.class);
        return annotations.stream().allMatch(DbMigration::enabled);
    }
}

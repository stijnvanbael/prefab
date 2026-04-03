package be.appify.prefab.processor.mongodb;

import be.appify.prefab.core.annotations.Indexed;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.Filters;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;

import java.util.List;

/**
 * Prefab plugin that generates a MongoDB {@code @Configuration} class to create indexes at startup
 * for fields annotated with {@link Indexed} or {@link Filter}.
 *
 * <p>The configuration is only generated when {@code MongoTemplate} is present on the compilation
 * classpath, i.e. when the {@code prefab-mongodb} (or equivalent) module is a project dependency.
 */
public class MongoIndexPlugin implements PrefabPlugin {

    private static final boolean MONGO_INCLUDED = isMongoIncluded();

    private PrefabContext context;

    /** Constructs a new MongoIndexPlugin. */
    public MongoIndexPlugin() {
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
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        if (!MONGO_INCLUDED) {
            return;
        }
        var manifestsWithIndexes = manifests.stream()
                .filter(MongoIndexPlugin::hasIndexedFields)
                .toList();
        if (!manifestsWithIndexes.isEmpty()) {
            new MongoIndexWriter(context).write(manifestsWithIndexes);
        }
    }

    private static boolean hasIndexedFields(ClassManifest manifest) {
        return manifest.fields().stream().anyMatch(field -> {
            if (field.hasAnnotation(Indexed.class)
                    || field.hasAnnotation(Filter.class)
                    || field.hasAnnotation(Filters.class)) {
                return true;
            }
            if (field.type().isRecord() && !field.type().isSingleValueType()) {
                return hasIndexedFields(field.type().asClassManifest());
            }
            return false;
        });
    }
}

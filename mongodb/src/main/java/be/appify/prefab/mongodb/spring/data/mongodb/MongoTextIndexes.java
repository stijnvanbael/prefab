package be.appify.prefab.mongodb.spring.data.mongodb;

import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

/**
 * Creates the text index of a collection. MongoDB refuses to create a text index when the collection already has a
 * text index with other options, so a change of the text language needs the old index to be dropped first. Used by
 * the generated index configuration.
 */
public final class MongoTextIndexes {
    private MongoTextIndexes() {
    }

    /**
     * Creates the text index, dropping an existing text index of the collection first when it has another default
     * language.
     *
     * @param indexOperations
     *         the index operations of the collection
     * @param definition
     *         the desired text index
     * @param language
     *         the default language of the desired text index
     */
    public static void createOrRebuild(
            IndexOperations indexOperations,
            TextIndexDefinition definition,
            String language
    ) {
        indexOperations.getIndexInfo().stream()
                .filter(info -> info.getIndexFields().stream().anyMatch(IndexField::isText))
                .filter(info -> !language.equals(info.getLanguage()))
                .map(IndexInfo::getName)
                .forEach(indexOperations::dropIndex);
        indexOperations.createIndex(definition);
    }
}

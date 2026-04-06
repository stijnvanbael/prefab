package be.appify.prefab.processor.mongodb;

/**
 * Represents a change to be applied to a MongoDB database as part of a migration.
 */
sealed interface MongoChange permits MongoChange.RenameCollection, MongoChange.RenameField {

    /** Returns the JavaScript statement that performs this change. */
    String toScript();

    /**
     * Renames a MongoDB collection.
     *
     * @param oldName the current (old) collection name
     * @param newName the desired (new) collection name
     */
    record RenameCollection(String oldName, String newName) implements MongoChange {
        @Override
        public String toScript() {
            return "db.getCollection(\"" + oldName + "\").renameCollection(\"" + newName + "\");\n";
        }

        @Override
        public String toString() {
            return toScript();
        }
    }

    /**
     * Renames a field (including nested dot-path fields) in all documents of a MongoDB collection.
     *
     * @param collection the collection to update
     * @param oldPath    the current field path (dot notation for nested fields)
     * @param newPath    the desired field path
     */
    record RenameField(String collection, String oldPath, String newPath) implements MongoChange {
        @Override
        public String toScript() {
            return "db.getCollection(\"" + collection + "\").updateMany({}, { $rename: { \""
                    + oldPath + "\": \"" + newPath + "\" } });\n";
        }

        @Override
        public String toString() {
            return toScript();
        }
    }
}

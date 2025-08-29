package be.appify.prefab.processor.dbmigration;

public interface ColumnModification {
    String toSql();

    record ChangeType(
            String name,
            DataType dataType
    ) implements ColumnModification {
        @Override
        public String toSql() {
            return "TYPE %s USING %s::%s".formatted(dataType, name, dataType);
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record ChangeNotNull(String name, boolean nullable, String defaultValue) implements ColumnModification {
        @Override
        public String toSql() {
            if (!nullable && defaultValue == null) {
                throw new IllegalStateException(
                        "Cannot set column [%s] to NOT NULL without a default value. Annotate the corresponding field with @DbDefaultValue to specify it."
                                .formatted(name));
            }
            return (nullable ? "DROP" : "SET") + " NOT NULL" +
                   (nullable ? "" : " DEFAULT " + defaultValue);
        }

        @Override
        public String toString() {
            return toSql();
        }
    }
}

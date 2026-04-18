package be.appify.prefab.processor.dbmigration;

interface ColumnModification {
    String toSql();

    record ChangeType(
            String name,
            DataType dataType
    ) implements ColumnModification {
        @Override
        public String toSql() {
            return "TYPE %s USING CAST(%s AS %s)".formatted(dataType, name, dataType);
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record SetDefault(String formattedValue) implements ColumnModification {
        @Override
        public String toSql() {
            return "SET DEFAULT " + formattedValue;
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record DropDefault() implements ColumnModification {
        @Override
        public String toSql() {
            return "DROP DEFAULT";
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record SetNotNull() implements ColumnModification {
        @Override
        public String toSql() {
            return "SET NOT NULL";
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record DropNotNull() implements ColumnModification {
        @Override
        public String toSql() {
            return "DROP NOT NULL";
        }

        @Override
        public String toString() {
            return toSql();
        }
    }
}

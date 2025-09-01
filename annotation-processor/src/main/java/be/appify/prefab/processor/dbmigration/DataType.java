package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.AnnotationManifest;
import be.appify.prefab.processor.TypeManifest;
import jakarta.validation.constraints.Size;

import java.io.File;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface DataType {
    String toSql();

    static DataType parse(String sql) {
        sql = sql.trim();
        if (sql.endsWith("[]")) {
            return Array.parse(sql);
        } else if (sql.startsWith("VARCHAR (") && sql.endsWith(")")) {
            return Varchar.parse(sql);
        } else if (sql.startsWith("DECIMAL (") && sql.endsWith(")")) {
            return Decimal.parse(sql);
        } else {
            try {
                return Primitive.valueOf(sql);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported data type: " + sql, e);
            }
        }
    }

    static DataType typeOf(TypeManifest type, List<AnnotationManifest> annotations) {
        if (type.is(String.class) || type.is(Reference.class) || type.isEnum() || type.is(Duration.class)) {
            var length = annotations.stream()
                    .filter(annotation -> annotation.type().is(Size.class))
                    .map(annotation -> (Integer) annotation.value("max"))
                    .findFirst().orElse(255);
            return new Varchar(length);
        } else if (type.is(Integer.class)) {
            return Primitive.INTEGER;
        } else if (type.is(Long.class)) {
            return Primitive.BIGINT;
        } else if (type.is(Boolean.class)) {
            return Primitive.BOOLEAN;
        } else if (type.is(BigDecimal.class) || type.is(Double.class) || type.is(Float.class)) {
            return new Decimal(19, 4);
        } else if (type.is(Instant.class) || type.is(OffsetDateTime.class)) {
            return Primitive.TIMESTAMP;
        } else if (type.is(LocalDate.class)) {
            return Primitive.DATE;
        } else if (type.is(byte[].class) || type.is(File.class)) {
            return Primitive.BYTEA;
        } else if (type.is(List.class)) {
            return new Array(typeOf(type.parameters().getFirst(), annotations));
        } else {
            throw new IllegalArgumentException("Unsupported type [%s]".formatted(type));
        }
    }

    enum Primitive implements DataType {
        INTEGER,
        BIGINT,
        BOOLEAN,
        TIMESTAMP,
        DATE,
        BYTEA;

        @Override
        public String toSql() {
            return name();
        }
    }

    record Array(DataType elementType) implements DataType {
        @Override
        public String toSql() {
            return elementType.toSql() + "[]";
        }

        static DataType parse(String sql) {
            var elementTypeSql = sql.substring(0, sql.length() - 2).trim();
            return new Array(DataType.parse(elementTypeSql));
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record Varchar(int length) implements DataType {
        @Override
        public String toSql() {
            return "VARCHAR (" + length + ")";
        }

        static DataType parse(String sql) {
            var lengthStr = sql.substring(9, sql.length() - 1).trim();
            var length = Integer.parseInt(lengthStr);
            return new Varchar(length);
        }

        @Override
        public String toString() {
            return toSql();
        }
    }

    record Decimal(int precision, int scale) implements DataType {
        @Override
        public String toSql() {
            return "DECIMAL (" + precision + ", " + scale + ")";
        }

        static DataType parse(String sql) {
            var paramsStr = sql.substring(9, sql.length() - 1).trim();
            var params = paramsStr.split(",");
            if (params.length != 2) {
                throw new IllegalArgumentException("Invalid DECIMAL type: " + sql);
            }
            var precision = Integer.parseInt(params[0].trim());
            var scale = Integer.parseInt(params[1].trim());
            return new Decimal(precision, scale);
        }

        @Override
        public String toString() {
            return toSql();
        }
    }
}
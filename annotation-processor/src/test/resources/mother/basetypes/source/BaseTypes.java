package mother.basetypes.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Example;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record BaseTypes(
        @Id String id,
        @Version long version,
        @Example("1") byte primitiveByte,
        @Example("2") Byte boxedByte,
        @Example("3") short primitiveShort,
        @Example("4") Short boxedShort,
        @Example("5") int primitiveInt,
        @Example("6") Integer boxedInt,
        @Example("7") long primitiveLong,
        @Example("8") Long boxedLong,
        @Example("1.5") double primitiveDouble,
        @Example("2.5") Double boxedDouble,
        @Example("3.5") float primitiveFloat,
        @Example("4.5") Float boxedFloat,
        @Example("true") boolean primitiveBoolean,
        @Example("false") Boolean boxedBoolean,
        @Example("a") char primitiveChar,
        @Example("b") Character boxedChar,
        @Example("12.34") BigDecimal amount,
        @Example("1691234567890") Instant createdAtFromMillis,
        @Example("2026-08-05T08:05:00+02:00") Instant createdAtFromIso,
        @Example("2026-08-05") LocalDate businessDate,
        @Example("2026-08-05T08:05:00") LocalDateTime createdOn,
        @Example("PT15M") Duration timeout,
        @Example("DONE") Status status) {

    public enum Status {
        TODO,
        DONE
    }

    @Create
    public BaseTypes(
            @Example("1") byte primitiveByte,
            @Example("2") Byte boxedByte,
            @Example("3") short primitiveShort,
            @Example("4") Short boxedShort,
            @Example("5") int primitiveInt,
            @Example("6") Integer boxedInt,
            @Example("7") long primitiveLong,
            @Example("8") Long boxedLong,
            @Example("1.5") double primitiveDouble,
            @Example("2.5") Double boxedDouble,
            @Example("3.5") float primitiveFloat,
            @Example("4.5") Float boxedFloat,
            @Example("true") boolean primitiveBoolean,
            @Example("false") Boolean boxedBoolean,
            @Example("a") char primitiveChar,
            @Example("b") Character boxedChar,
            @Example("12.34") BigDecimal amount,
            @Example("1691234567890") Instant createdAtFromMillis,
            @Example("2026-08-05T08:05:00+02:00") Instant createdAtFromIso,
            @Example("2026-08-05") LocalDate businessDate,
            @Example("2026-08-05T08:05:00") LocalDateTime createdOn,
            @Example("PT15M") Duration timeout,
            @Example("DONE") Status status) {
        this(UUID.randomUUID().toString(), 0L,
                primitiveByte,
                boxedByte,
                primitiveShort,
                boxedShort,
                primitiveInt,
                boxedInt,
                primitiveLong,
                boxedLong,
                primitiveDouble,
                boxedDouble,
                primitiveFloat,
                boxedFloat,
                primitiveBoolean,
                boxedBoolean,
                primitiveChar,
                boxedChar,
                amount,
                createdAtFromMillis,
                createdAtFromIso,
                businessDate,
                createdOn,
                timeout,
                status);
    }

    @Update
    public BaseTypes update(
            @Example("1") byte primitiveByte,
            @Example("2") Byte boxedByte,
            @Example("3") short primitiveShort,
            @Example("4") Short boxedShort,
            @Example("5") int primitiveInt,
            @Example("6") Integer boxedInt,
            @Example("7") long primitiveLong,
            @Example("8") Long boxedLong,
            @Example("1.5") double primitiveDouble,
            @Example("2.5") Double boxedDouble,
            @Example("3.5") float primitiveFloat,
            @Example("4.5") Float boxedFloat,
            @Example("true") boolean primitiveBoolean,
            @Example("false") Boolean boxedBoolean,
            @Example("a") char primitiveChar,
            @Example("b") Character boxedChar,
            @Example("12.34") BigDecimal amount,
            @Example("1691234567890") Instant createdAtFromMillis,
            @Example("2026-08-05T08:05:00+02:00") Instant createdAtFromIso,
            @Example("2026-08-05") LocalDate businessDate,
            @Example("2026-08-05T08:05:00") LocalDateTime createdOn,
            @Example("PT15M") Duration timeout,
            @Example("DONE") Status status) {
        return new BaseTypes(id, version,
                primitiveByte,
                boxedByte,
                primitiveShort,
                boxedShort,
                primitiveInt,
                boxedInt,
                primitiveLong,
                boxedLong,
                primitiveDouble,
                boxedDouble,
                primitiveFloat,
                boxedFloat,
                primitiveBoolean,
                boxedBoolean,
                primitiveChar,
                boxedChar,
                amount,
                createdAtFromMillis,
                createdAtFromIso,
                businessDate,
                createdOn,
                timeout,
                status);
    }
}

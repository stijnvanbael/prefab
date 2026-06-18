package event.avsc.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import java.io.IOException;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component("event_avsc_NullableAvscEventSchemaFactory")
public class NullableAvscEventSchemaFactory {
    private final Schema schema;

    public NullableAvscEventSchemaFactory() {
        this.schema = loadExpectedSchema();
    }

    private static Schema loadExpectedSchema() {
        try (var stream = NullableAvscEventSchemaFactory.class.getClassLoader().getResourceAsStream("event/avsc/nullable/source/NullableAvscEvent.avsc")) {
            if (stream == null) {
                throw new IllegalStateException("AVSC file not found on classpath: event/avsc/nullable/source/NullableAvscEvent.avsc");
            }
            var parsedSchema = new Schema.Parser().parse(stream);
            return SchemaSupport.namedTypeOf(parsedSchema, "NullableAvscEvent");
        } catch (IOException e) {
            throw new IllegalStateException("Could not read AVSC 'event/avsc/nullable/source/NullableAvscEvent.avsc' for 'NullableAvscEvent': " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not validate AVSC 'event/avsc/nullable/source/NullableAvscEvent.avsc' for 'NullableAvscEvent': " + e.getMessage(), e);
        }
    }

    public Schema createSchema() {
        return this.schema;
    }
}

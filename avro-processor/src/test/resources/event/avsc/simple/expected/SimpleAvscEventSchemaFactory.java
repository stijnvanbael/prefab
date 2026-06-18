package event.avsc.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import java.io.IOException;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component("event_avsc_SimpleAvscEventSchemaFactory")
public class SimpleAvscEventSchemaFactory {
    private final Schema schema;

    public SimpleAvscEventSchemaFactory() {
        this.schema = loadExpectedSchema();
    }

    private static Schema loadExpectedSchema() {
        try (var stream = SimpleAvscEventSchemaFactory.class.getClassLoader().getResourceAsStream("event/avsc/simple/source/SimpleAvscEvent.avsc")) {
            if (stream == null) {
                throw new IllegalStateException("AVSC file not found on classpath: event/avsc/simple/source/SimpleAvscEvent.avsc");
            }
            var parsedSchema = new Schema.Parser().parse(stream);
            return SchemaSupport.namedTypeOf(parsedSchema, "SimpleAvscEvent");
        } catch (IOException e) {
            throw new IllegalStateException("Could not read AVSC 'event/avsc/simple/source/SimpleAvscEvent.avsc' for 'SimpleAvscEvent': " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not validate AVSC 'event/avsc/simple/source/SimpleAvscEvent.avsc' for 'SimpleAvscEvent': " + e.getMessage(), e);
        }
    }

    public Schema createSchema() {
        return this.schema;
    }
}

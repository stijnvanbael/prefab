package be.appify.prefab.avro.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

import static be.appify.prefab.avro.processor.AvroSupport.componentAnnotation;
import static be.appify.prefab.avro.processor.EventSchemaFactoryWriter.schemaFactoryClassName;

/**
 * Generates a component per Avro event that registers the schema of the event as the reader schema, so that Kafka
 * consumers resolve the schema a message was written with against the current schema of the event.
 */
class ReaderSchemaRegistrarWriter {
    private static final String AVRO_PACKAGE_SUFFIX = "infrastructure.avro";

    private final PrefabContext context;

    ReaderSchemaRegistrarWriter(PrefabContext context) {
        this.context = context;
    }

    void writeRegistrar(TypeManifest event) {
        var name = "%sReaderSchemaRegistrar".formatted(event.simpleName().replace(".", ""));
        var factory = schemaFactoryClassName(event);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation(event, name))
                .addSuperinterface(EventRegistryCustomizer.class)
                .addField(FieldSpec.builder(factory, "schemaFactory", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(factory, "schemaFactory")
                        .addStatement("this.schemaFactory = schemaFactory")
                        .build())
                .addMethod(MethodSpec.methodBuilder("customize")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(EventRegistry.class, "registry")
                        .addStatement("registry.registerReaderSchema($T.class, schemaFactory.createSchema())",
                                event.asTypeName())
                        .build())
                .build();
        new OutputTargetFileOutput(context, AVRO_PACKAGE_SUFFIX, OutputTarget.MAIN)
                .writeFile(event.packageName(), name, type);
    }
}

package be.appify.prefab.avro.processor;

import be.appify.prefab.core.annotations.OutputTarget;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import be.appify.prefab.processor.OutputTargetFileOutput;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.TypeManifest;
import be.appify.prefab.processor.event.EventBindings;
import com.palantir.javapoet.ClassName;
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
        var name = "%sReaderSchemaRegistrar".formatted(AvroSupport.generatedName(event));
        var factory = schemaFactoryClassName(event);
        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(componentAnnotation(event, name))
                .addSuperinterface(EventRegistryCustomizer.class)
                .addField(FieldSpec.builder(factory, "schemaFactory", Modifier.PRIVATE, Modifier.FINAL).build());
        var constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                .addParameter(factory, "schemaFactory")
                .addStatement("this.schemaFactory = schemaFactory");
        var customize = MethodSpec.methodBuilder("customize").addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class).addParameter(EventRegistry.class, "registry")
                .addStatement("registry.registerReaderSchema($L, schemaFactory.createSchema())",
                        EventBindings.typeReference(event.asTypeName()));
        if (!event.parameters().isEmpty()) {
            var writer = ClassName.get(event.packageName() + ".infrastructure.avro",
                    AvroSupport.generatedName(event) + "ToGenericRecordConverter");
            var reader = ClassName.get(event.packageName() + ".infrastructure.avro",
                    "GenericRecordTo" + AvroSupport.generatedName(event) + "Converter");
            type.addField(writer, "writer", Modifier.PRIVATE, Modifier.FINAL)
                    .addField(reader, "reader", Modifier.PRIVATE, Modifier.FINAL);
            constructor.addParameter(writer, "writer").addParameter(reader, "reader")
                    .addStatement("this.writer = writer").addStatement("this.reader = reader");
            customize.addStatement("var type = $L", EventBindings.typeReference(event.asTypeName()))
                    .addStatement("registry.registerAvroConverters(type, writer::convert, reader::convert)");
        }
        type.addMethod(constructor.build()).addMethod(customize.build());
        new OutputTargetFileOutput(context, AVRO_PACKAGE_SUFFIX, OutputTarget.MAIN)
                .writeFile(event.packageName(), name, type.build());
    }
}

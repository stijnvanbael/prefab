package be.appify.prefab.mongodb.spring.data.mongodb;

import be.appify.prefab.core.service.Reference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Reading converter that deserialises a plain string from a MongoDB document into a {@link Reference} value.
 */
@ReadingConverter
public class StringToReferenceConverter implements Converter<String, Reference<?>> {

    /** Constructs a new StringToReferenceConverter. */
    public StringToReferenceConverter() {
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Reference convert(String source) {
        return new Reference<>(source);
    }
}

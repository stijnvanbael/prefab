package be.appify.prefab.mongodb.spring.data.mongodb;

import be.appify.prefab.core.service.Reference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Writing converter that serialises a {@link Reference} value to its underlying {@code id} string so that
 * references are stored as plain strings in MongoDB documents instead of sub-documents.
 */
@WritingConverter
public class ReferenceToStringConverter implements Converter<Reference<?>, String> {

    /** Constructs a new ReferenceToStringConverter. */
    public ReferenceToStringConverter() {
    }

    @Override
    public String convert(Reference<?> source) {
        return source.id();
    }
}

package be.appify.prefab.core.spring.converters;

import be.appify.prefab.core.service.Reference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/** Converter to transform a Reference into its String ID representation. */
@WritingConverter
public class ReferenceToStringConverter implements Converter<Reference<?>, String> {

    /** Constructs a new ReferenceToStringConverter. */
    public ReferenceToStringConverter() {
    }

    @Override
    public String convert(Reference<?> source) {
        return source != null ? source.id() : null;
    }
}

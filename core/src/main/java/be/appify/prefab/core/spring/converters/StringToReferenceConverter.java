package be.appify.prefab.core.spring.converters;

import be.appify.prefab.core.service.Reference;
import java.util.Set;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.convert.ReadingConverter;

/** Converter to transform a String into a Reference. */
@ReadingConverter
public class StringToReferenceConverter implements GenericConverter {

    /**
     * Constructs a new StringToReferenceConverter.
     */
    public StringToReferenceConverter() {
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Set.of(new ConvertiblePair(String.class, Reference.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        } else {
            return Reference.fromId(source.toString());
        }
    }
}

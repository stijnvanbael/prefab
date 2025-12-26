package be.appify.prefab.core.spring.converters;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.spring.ReferenceFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.convert.ReadingConverter;

import java.util.Set;

@ReadingConverter
public class StringToReferenceConverter implements GenericConverter {
    private final ReferenceFactory referenceFactory;

    public StringToReferenceConverter(ReferenceFactory referenceFactory) {
        this.referenceFactory = referenceFactory;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Set.of(new ConvertiblePair(String.class, Reference.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        var type = targetType.getResolvableType().getGenerics()[0].getType();
        return referenceFactory.referenceTo((Class<?>) type, (String) source);
    }
}

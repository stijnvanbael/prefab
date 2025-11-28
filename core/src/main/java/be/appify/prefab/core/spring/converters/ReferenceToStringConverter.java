package be.appify.prefab.core.spring.converters;

import be.appify.prefab.core.service.Reference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class ReferenceToStringConverter implements Converter<Reference<?>, String> {

    @Override
    public String convert(Reference<?> source) {
        return source.id();
    }
}

package dbmigration.dbcolumn;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class FloatArrayToVectorConverter implements Converter<float[], String> {
    @Override
    public String convert(float[] source) {
        return "[]";
    }
}


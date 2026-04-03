package persistence.polymorphic.infrastructure.persistence;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.spring.data.jdbc.PolymorphicReadingConverter;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.stereotype.Component;
import persistence.polymorphic.Shape;

@Component
@ReadingConverter
public class ShapeReadingConverter implements Converter<Map<String, Object>, Shape>, PolymorphicReadingConverter {
    private final JdbcConverter converter;

    ShapeReadingConverter(JdbcConverter converter) {
        this.converter = converter;
    }

    @Override
    public Shape convert(Map<String, Object> row) {
        var type = (String) row.get("type");
        return switch (type) {
                    case "Circle" -> new Shape.Circle(Reference.fromId((String) row.get("id")), (Long) converter.readValue(row.get("version"), ClassTypeInformation.from(Long.class)), (Double) converter.readValue(row.get("radius"), ClassTypeInformation.from(Double.class)));
                    case "Rectangle" -> new Shape.Rectangle(Reference.fromId((String) row.get("id")), (Long) converter.readValue(row.get("version"), ClassTypeInformation.from(Long.class)), (Double) converter.readValue(row.get("width"), ClassTypeInformation.from(Double.class)), (Double) converter.readValue(row.get("height"), ClassTypeInformation.from(Double.class)));
                    default -> throw new IllegalArgumentException("Unknown Shape type: " + type);
                };
    }
}

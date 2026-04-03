package persistence.polymorphic.infrastructure.persistence;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.postgres.spring.data.jdbc.PolymorphicReadingConverter;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;
import persistence.polymorphic.Shape;

@Component
@ReadingConverter
public class ShapeReadingConverter implements Converter<Map<String, Object>, Shape>, PolymorphicReadingConverter {
    @Override
    public Shape convert(Map<String, Object> row) {
        var type = (String) row.get("type");
        return switch (type) {
                    case "Circle" -> new Shape.Circle(Reference.fromId((String) row.get("id")), row.get("version") instanceof Number n ? n.longValue() : 0L, row.get("radius") instanceof Number n ? n.doubleValue() : 0.0);
                    case "Rectangle" -> new Shape.Rectangle(Reference.fromId((String) row.get("id")), row.get("version") instanceof Number n ? n.longValue() : 0L, row.get("width") instanceof Number n ? n.doubleValue() : 0.0, row.get("height") instanceof Number n ? n.doubleValue() : 0.0);
                    default -> throw new IllegalArgumentException("Unknown Shape type: " + type);
                };
    }
}

package persistence.polymorphic.infrastructure.persistence;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.core.spring.data.jdbc.PolymorphicReadingConverter;
import java.util.Map;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;
import persistence.polymorphic.Shape;

@Component
@ReadingConverter
public class ShapeReadingConverter implements Converter<Map<String, Object>, Shape>, PolymorphicReadingConverter {
    private final ConversionService conversionService;

    ShapeReadingConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Shape convert(Map<String, Object> row) {
        var type = (String) row.get("type");
        if (type == null) {
            throw new IllegalArgumentException("Missing type discriminator for Shape");
        }
        return switch (type) {
                    case "Circle" -> new Shape.Circle(Reference.fromId((String) row.get("id")), conversionService.convert(row.get("version"), Long.class), conversionService.convert(row.get("radius"), Double.class));
                    case "Rectangle" -> new Shape.Rectangle(Reference.fromId((String) row.get("id")), conversionService.convert(row.get("version"), Long.class), conversionService.convert(row.get("width"), Double.class), conversionService.convert(row.get("height"), Double.class));
                    default -> throw new IllegalArgumentException("Unknown Shape type: " + type);
                };
    }
}

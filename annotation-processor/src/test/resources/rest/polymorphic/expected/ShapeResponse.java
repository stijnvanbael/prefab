package rest.polymorphic.infrastructure.http;

import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import rest.polymorphic.Shape;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({@JsonSubTypes.Type(value = ShapeResponse.CircleResponse.class, name = "Circle"), @JsonSubTypes.Type(value = ShapeResponse.RectangleResponse.class, name = "Rectangle")})
public sealed interface ShapeResponse permits ShapeResponse.CircleResponse, ShapeResponse.RectangleResponse {
    static ShapeResponse from(Shape aggregate) {
        return switch (aggregate) {
                case Shape.Circle t -> CircleResponse.from(t);
                case Shape.Rectangle t -> RectangleResponse.from(t);
                };
    }

    record CircleResponse(Reference<Shape> id, long version,
            double radius) implements ShapeResponse {
        public static CircleResponse from(Shape.Circle subtype) {
            return new CircleResponse(subtype.id(),
                    subtype.version(),
                    subtype.radius());
        }
    }

    record RectangleResponse(Reference<Shape> id, long version, double width,
            double height) implements ShapeResponse {
        public static RectangleResponse from(Shape.Rectangle subtype) {
            return new RectangleResponse(subtype.id(),
                    subtype.version(),
                    subtype.width(),
                    subtype.height());
        }
    }
}

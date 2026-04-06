package be.appify.prefab.example.mongodb.shape;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** Polymorphic shape aggregate. Demonstrates sealed-interface @Aggregate support. */
@Aggregate
@GetById
@GetList
public sealed interface Shape permits Shape.Circle, Shape.Rectangle {

    /** A circular shape with a radius. */
    record Circle(
            @Id Reference<Shape> id,
            @Version long version,
            double radius
    ) implements Shape {

        /** Creates a new Circle with the given radius. */
        public Circle(double radius) {
            this(Reference.create(), 0L, radius);
        }
    }

    /** A rectangular shape with width and height. */
    record Rectangle(
            @Id Reference<Shape> id,
            @Version long version,
            double width,
            double height
    ) implements Shape {

        /** Creates a new Rectangle with the given width and height. */
        public Rectangle(double width, double height) {
            this(Reference.create(), 0L, width, height);
        }
    }
}

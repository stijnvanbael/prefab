package persistence.polymorphic_interface_annotations;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public sealed interface Shape permits Shape.Circle, Shape.Rectangle {

    @Id
    Reference<Shape> id();

    @Version
    long version();

    record Circle(
            Reference<Shape> id,
            long version,
            double radius
    ) implements Shape {
    }

    record Rectangle(
            Reference<Shape> id,
            long version,
            double width,
            double height
    ) implements Shape {
    }
}


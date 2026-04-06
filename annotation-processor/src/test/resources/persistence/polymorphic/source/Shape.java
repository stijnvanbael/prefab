package persistence.polymorphic;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public sealed interface Shape permits Shape.Circle, Shape.Rectangle {

    record Circle(
            @Id Reference<Shape> id,
            @Version long version,
            double radius
    ) implements Shape {
    }

    record Rectangle(
            @Id Reference<Shape> id,
            @Version long version,
            double width,
            double height
    ) implements Shape {
    }
}

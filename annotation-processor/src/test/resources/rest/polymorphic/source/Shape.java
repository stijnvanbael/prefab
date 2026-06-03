package rest.polymorphic;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
@GetById
@GetList
@Delete
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public sealed interface Shape permits Shape.Circle, Shape.Rectangle {

    record Circle(
            @Id Reference<Shape> id,
            @Version long version,
            double radius
    ) implements Shape {
        @Create
        public Circle(double radius) {
            this(Reference.create(), 0L, radius);
        }
    }

    record Rectangle(
            @Id Reference<Shape> id,
            @Version long version,
            double width,
            double height
    ) implements Shape {
        @Create
        public Rectangle(double width, double height) {
            this(Reference.create(), 0L, width, height);
        }
    }
}

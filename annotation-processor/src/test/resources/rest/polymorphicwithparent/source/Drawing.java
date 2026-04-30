package rest.polymorphicwithparent;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
@Delete
public sealed interface Drawing permits Drawing.Circle, Drawing.Rectangle {

    String id();

    long version();

    @Parent
    Reference<Canvas> canvas();

    record Circle(
            @Id String id,
            @Version long version,
            @Parent Reference<Canvas> canvas,
            double radius
    ) implements Drawing {

        @Create
        public Circle(Reference<Canvas> canvas, double radius) {
            this(UUID.randomUUID().toString(), 0L, canvas, radius);
        }

        @Update
        public Circle resize(Reference<Canvas> canvas, double radius) {
            return new Circle(id, version, canvas, radius);
        }
    }

    record Rectangle(
            @Id String id,
            @Version long version,
            @Parent Reference<Canvas> canvas,
            double width,
            double height
    ) implements Drawing {

        @Create
        public Rectangle(Reference<Canvas> canvas, double width, double height) {
            this(UUID.randomUUID().toString(), 0L, canvas, width, height);
        }

        @Update
        public Rectangle resize(Reference<Canvas> canvas, double width, double height) {
            return new Rectangle(id, version, canvas, width, height);
        }
    }
}


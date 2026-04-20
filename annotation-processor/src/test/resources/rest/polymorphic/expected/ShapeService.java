package rest.polymorphic.application;

import jakarta.validation.Valid;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rest.polymorphic.Shape;

@Component
@Transactional
public class ShapeService {
    private static final Logger log = LoggerFactory.getLogger(ShapeService.class);

    private final ShapeRepository shapeRepository;

    public ShapeService(ShapeRepository shapeRepository) {
        this.shapeRepository = shapeRepository;
    }

    public String createCircle(@Valid CreateCircleRequest request) {
        log.debug("Creating new {}", Shape.Circle.class.getSimpleName());
        var aggregate = new Shape.Circle(request.radius());
        shapeRepository.save(aggregate);
        return aggregate.id().id();
    }

    public String createRectangle(@Valid CreateRectangleRequest request) {
        log.debug("Creating new {}", Shape.Rectangle.class.getSimpleName());
        var aggregate = new Shape.Rectangle(request.width(), request.height());
        shapeRepository.save(aggregate);
        return aggregate.id().id();
    }

    public Optional<Shape> getById(String id) {
        log.debug("Getting {} by id: {}", Shape.class.getSimpleName(), id);
        return shapeRepository.findById(id);
    }

    public void delete(String id) {
        log.debug("Deleting {} with id: {}", Shape.class.getSimpleName(), id);
        shapeRepository.deleteById(id);
    }

    public Page<Shape> getList(Pageable pageable) {
        log.debug("Getting Shapes");
        return shapeRepository.findAll(pageable);
    }
}

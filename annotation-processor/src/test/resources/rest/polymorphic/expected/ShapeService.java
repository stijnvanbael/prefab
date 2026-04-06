package rest.polymorphic.application;

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

    public Optional<Shape> getById(String id) {
        log.debug("Getting {} by id: {}", Shape.class.getSimpleName(), id);
        return shapeRepository.findById(id);
    }

    public Page<Shape> getList(Pageable pageable) {
        log.debug("Getting Shapes");
        return shapeRepository.findAll(pageable);
    }
}

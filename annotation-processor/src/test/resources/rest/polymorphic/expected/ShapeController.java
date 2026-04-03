package rest.polymorphic.infrastructure.http;

import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rest.polymorphic.Shape;
import rest.polymorphic.application.ShapeService;

@RestController
@RequestMapping(
        path = "shapes"
)
public class ShapeController {
    private final ShapeService service;

    ShapeController(ShapeService service) {
        this.service = service;
    }

    private static ResponseEntity<ShapeResponse> toResponse(Optional<Shape> aggregateRoot) {
        return aggregateRoot
                    .map(ShapeResponse::from)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = "/{id}"
    )
    public ResponseEntity<ShapeResponse> getById(@PathVariable String id) {
        return toResponse(service.getById(id));
    }

    @RequestMapping(
            method = RequestMethod.GET,
            path = ""
    )
    public ResponseEntity<PagedModel<ShapeResponse>> getList(Pageable pageable) {
        return ResponseEntity.ok(new PagedModel(service.getList(pageable).map(ShapeResponse::from)));
    }
}

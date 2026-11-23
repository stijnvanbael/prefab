package handwritten.source;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SuppressWarnings("prefab.handwritten")
class LegacyOrderController {

    @DeleteMapping("/orders/{id}")
    void delete(@PathVariable String id) {
    }
}

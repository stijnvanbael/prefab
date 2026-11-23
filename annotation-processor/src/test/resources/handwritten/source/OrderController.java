package handwritten.source;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Takes over the generated controller through the source-file override. */
@RestController
class OrderController {

    @GetMapping("/orders/{id}")
    String get(@PathVariable String id) {
        return id;
    }
}

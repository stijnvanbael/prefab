package handwritten.source;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
class ClashingOrderController {

    @GetMapping("/{orderId}")
    String get(@PathVariable String orderId) {
        return orderId;
    }

    @PostMapping
    String create() {
        return "created";
    }
}

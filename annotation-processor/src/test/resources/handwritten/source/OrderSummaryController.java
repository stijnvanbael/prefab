package handwritten.source;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
class OrderSummaryController {

    @GetMapping("/{id}/summary")
    String summary(@PathVariable String id) {
        return id;
    }

    @PutMapping(path = "/{id}/archive")
    void archive(@PathVariable String id) {
    }
}

package handwritten.source;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AnyMethodController {

    @RequestMapping("/orders")
    String anything() {
        return "anything";
    }
}

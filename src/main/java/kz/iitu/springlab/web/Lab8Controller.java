package kz.iitu.springlab.web;
import kz.iitu.springlab.catalog.Lab8Evidence;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/lab8")
public class Lab8Controller {
    private final Lab8Evidence evidence;
    public Lab8Controller(Lab8Evidence evidence) { this.evidence = evidence; }
    @GetMapping("/evidence")
    public Map<String, Object> evidence() { return evidence.getResults(); }
}

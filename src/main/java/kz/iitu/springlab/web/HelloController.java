package kz.iitu.springlab.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Value("${app.owner:unknown}")
    private String owner;

    @GetMapping("/hello")
    public Greeting hello(@RequestParam(defaultValue = "world") String name) {
        return new Greeting("Hello, " + name + "!", owner, LocalDateTime.now());
    }

    @GetMapping("/info")
    public Info info() {
        return new Info(owner,
                System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors());
    }
    @GetMapping("/case")
    public CaseResult caseConvert(@RequestParam(defaultValue = "") String text) {
        String upper = text.toUpperCase();
        String lower = text.toLowerCase();
        StringBuilder title = new StringBuilder();
        for (String w : text.trim().split("\\s+")) {
            if (w.isEmpty()) continue;
            if (title.length() > 0) title.append(" ");
            title.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) title.append(w.substring(1).toLowerCase());
        }
        return new CaseResult(text, upper, lower, title.toString(), text.length());
    }

    public record CaseResult(String original, String upper, String lower, String title, int length) { }
    public record Greeting(String message, String owner, LocalDateTime timestamp) { }
    public record Info(String owner, String javaVersion, int cpuCores) { }
}
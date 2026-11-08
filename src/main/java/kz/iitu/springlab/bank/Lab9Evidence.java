package kz.iitu.springlab.bank;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Profile("dev")
public class Lab9Evidence implements CommandLineRunner {
    private final Lab9Experiments experiments;
    private Map<String, Object> results = Map.of();
    public Lab9Evidence(Lab9Experiments experiments) { this.experiments = experiments; }
    public Map<String, Object> getResults() { return Collections.unmodifiableMap(results); }

    @Override
    public void run(String... args) {
        results = experiments.runAll();
        System.out.println("LAB09_RESULTS " + new tools.jackson.databind.json.JsonMapper().writeValueAsString(results));
    }
}

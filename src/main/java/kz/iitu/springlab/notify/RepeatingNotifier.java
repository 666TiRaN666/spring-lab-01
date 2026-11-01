package kz.iitu.springlab.notify;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component("repeating")
@Order(3)
public class RepeatingNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(RepeatingNotifier.class);

    private final int times;

    public RepeatingNotifier(@Value("${app.repeating-times:3}") int times) {
        this.times = times;
    }

    @PostConstruct
    void init() {
        log.info("REPEATING initialised, times={}", times);
    }

    @Override
    public String send(String message) {
        if (times <= 1) {
            return "repeating: " + message;
        }
        return "repeating: " + String.join(" ", Collections.nCopies(times, message));
    }

    @Override
    public String channel() {
        return "repeating";
    }
}

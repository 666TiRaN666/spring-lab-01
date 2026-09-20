package kz.iitu.springlab.web;

import kz.iitu.springlab.lifecycle.LifecycleDemo;
import kz.iitu.springlab.notify.NotificationService;
import kz.iitu.springlab.notify.Notifier;
import kz.iitu.springlab.scope.TicketOffice;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab2")
public class Lab2Controller {

    private final NotificationService notifications;
    private final LifecycleDemo lifecycle;
    private final TicketOffice tickets;
    private final Notifier repeating;

    public Lab2Controller(NotificationService notifications,
                          LifecycleDemo lifecycle,
                          TicketOffice tickets,
                          @Qualifier("repeating") Notifier repeating) {
        this.notifications = notifications;
        this.lifecycle = lifecycle;
        this.tickets = tickets;
        this.repeating = repeating;
    }

    @GetMapping("/notify")
    public Map<String, Object> notify(@RequestParam(defaultValue = "Hello") String text) {
        return Map.of("primary", notifications.viaPrimary(text),
                      "console", notifications.viaConsole(text),
                      "all", notifications.viaAll(text),
                      "beanNames", notifications.names());
    }

    @GetMapping("/lifecycle")
    public List<String> lifecycle() {
        return lifecycle.events();
    }

    @GetMapping("/scopes")
    public Map<String, Object> scopes() {
        return tickets.demo();
    }

    @GetMapping("/custom")
    public Map<String, Object> custom(@RequestParam(defaultValue = "Hello") String text) {
        return Map.of("channel", repeating.channel(),
                      "result", repeating.send(text));
    }
}

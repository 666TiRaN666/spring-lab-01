package kz.iitu.springlab.service;

import kz.iitu.springlab.audit.Audited;
import kz.iitu.springlab.ratelimit.RateLimit;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class CatalogService {

    private final ItemRemovalService removalService;

    public CatalogService(ItemRemovalService removalService) {
        this.removalService = removalService;
    }

    @RateLimit(perMinute = 5)
    public String findById(long id) {
        sleep(50);                              // simulating a database call
        return "Item no. " + id;
    }

    @Audited(action = "CATALOG_LIST", logArguments = true)
    public List<String> findAll(int limit) {
        sleep(300);                             // a deliberately «slow» method
        return IntStream.rangeClosed(1, limit)
                .mapToObj(i -> "Item no. " + i)
                .toList();
    }

    // remove() lives in ItemRemovalService (clean fix for self-invocation).
    // Kept here only as a delegating shortcut for old callers.
    public String remove(long id) {
        return removalService.remove(id);
    }

    // FIXED version: neighbouring calls go through the separate bean proxy,
    // so Logging/Timing/Audit aspects fire for each remove.
    public String removeTwice(long id) {
        String first = removalService.remove(id);
        String second = removalService.remove(id + 1);
        return first + "; " + second;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

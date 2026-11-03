package kz.iitu.springlab.web;

import kz.iitu.springlab.service.CatalogService;
import kz.iitu.springlab.service.ItemRemovalService;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab4")
public class CatalogController {

    private final CatalogService catalogService;
    private final ItemRemovalService removalService;

    public CatalogController(CatalogService catalogService,
                             ItemRemovalService removalService) {
        this.catalogService = catalogService;
        this.removalService = removalService;
    }

    @GetMapping("/item/{id}")
    public String item(@PathVariable long id) {
        return catalogService.findById(id);
    }

    @GetMapping("/items")
    public List<String> items(@RequestParam(defaultValue = "5") int limit) {
        return catalogService.findAll(limit);
    }

    @DeleteMapping("/item/{id}")
    public String delete(@PathVariable long id) {
        // Direct call to the separate bean: single interception, no doubling.
        return removalService.remove(id);
    }

    @GetMapping("/proxy")
    public Map<String, String> proxyInfo() {
        return Map.of(
                "className", catalogService.getClass().getName(),
                "superClass", catalogService.getClass().getSuperclass().getSimpleName(),
                "isAopProxy", String.valueOf(AopUtils.isAopProxy(catalogService)),
                "isCglib", String.valueOf(AopUtils.isCglibProxy(catalogService)));
    }

    @GetMapping("/remove-twice/{id}")
    public String removeTwice(@PathVariable long id) {
        return catalogService.removeTwice(id);
    }
}

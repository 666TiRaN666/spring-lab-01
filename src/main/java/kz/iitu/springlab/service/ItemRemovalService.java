package kz.iitu.springlab.service;

import kz.iitu.springlab.audit.Audited;
import org.springframework.stereotype.Service;

// Separate bean for remove: clean fix for self-invocation.
// Calls via this bean always go through the proxy, so aspects fire.
@Service
public class ItemRemovalService {

    @Audited(action = "CATALOG_REMOVE")
    public String remove(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid identifier: " + id);
        }
        return "Removed item no. " + id;
    }
}

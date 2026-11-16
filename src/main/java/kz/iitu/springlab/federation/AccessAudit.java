package kz.iitu.springlab.federation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Task F audit trail: who, what, which resource, the decision, why, when and under which policy version.
 * The token id (jti) identifies the token; the bearer token itself is never stored or logged.
 */
@Component
public class AccessAudit {
    private static final Logger log = LoggerFactory.getLogger(AccessAudit.class);
    private static final int MAX = 200;
    private final List<Map<String, Object>> records = Collections.synchronizedList(new ArrayList<>());

    public void record(String subject, String action, String decision, String reason, String tokenId) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("timestamp", Instant.now().toString());
        r.put("subject", subject);
        r.put("action", action);
        r.put("decision", decision);
        r.put("reason", reason);
        r.put("policyVersion", TokenSettings.POLICY_VERSION);
        r.put("tokenId", tokenId);
        synchronized (records) {
            records.add(r);
            if (records.size() > MAX) records.removeFirst();
        }
        log.info("AUDIT {}", r);
    }

    public List<Map<String, Object>> records() {
        synchronized (records) { return List.copyOf(records); }
    }

    public void clear() { records.clear(); }
}

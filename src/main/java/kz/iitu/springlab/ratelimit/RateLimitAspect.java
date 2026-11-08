package kz.iitu.springlab.ratelimit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

// Variant 7: rate limiting, separate aspect class
@Aspect
@Component
@Order(4)
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final ConcurrentHashMap<String, Deque<Long>> calls = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object limit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = pjp.getSignature().toShortString();
        long now = System.currentTimeMillis();
        long window = 60_000L;
        Deque<Long> times = calls.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > window) {
                times.pollFirst();
            }
            if (times.size() >= rateLimit.perMinute()) {
                log.warn("[LIMIT] {} exceeded {} per minute", key, rateLimit.perMinute());
                throw new IllegalStateException(
                        "Rate limit exceeded for " + key + ": max "
                                + rateLimit.perMinute() + " per minute");
            }
            times.addLast(now);
        }
        return pjp.proceed();
    }
}

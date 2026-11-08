package kz.iitu.springlab.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
@Order(1)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        String args = audited.logArguments()
                ? " args=" + Arrays.toString(pjp.getArgs())
                : "";
        log.info("[AUDIT] start {} at {}{}",
                audited.action(), LocalDateTime.now(), args);
        try {
            Object result = pjp.proceed();
            log.info("[AUDIT] {} success at {}",
                    audited.action(), LocalDateTime.now());
            return result;
        } catch (Exception ex) {
            log.info("[AUDIT] {} failure at {}: {}",
                    audited.action(), LocalDateTime.now(), ex.getMessage());
            throw ex;
        }
    }
}

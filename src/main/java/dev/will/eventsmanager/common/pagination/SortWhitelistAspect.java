package dev.will.eventsmanager.common.pagination;

import dev.will.eventsmanager.common.exception.InvalidSortFieldException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Aspect
@Component
public class SortWhitelistAspect {

    @Before("@annotation(allowed)")
    public void validate(JoinPoint jp, AllowedSortFields allowed) {
        Set<String> whitelist = Set.of(allowed.value());
        for (Object arg : jp.getArgs()) {
            if (arg instanceof Pageable pageable) {
                pageable.getSort().forEach(order -> {
                    if (!whitelist.contains(order.getProperty())) {
                        throw new InvalidSortFieldException(order.getProperty());
                    }
                });
            }
        }
    }
}

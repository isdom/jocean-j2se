package org.jocean.j2se.tracing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.jocean.j2se.tracing.Tracing.Scope;

//该类为切面
public class TracingAspect {
    public void around(final ProceedingJoinPoint pjp) throws Throwable {
        final Object[] args = pjp.getArgs();
        if (args.length > 0 && args[0] instanceof Tracing) {
            final Tracing tracing = (Tracing) args[0];
            try (final Scope scope = tracing.activate()) {
                pjp.proceed();
            }
        } else {
            pjp.proceed();
        }
    }
}
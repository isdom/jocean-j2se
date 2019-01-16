package org.jocean.j2se.tracing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.jocean.j2se.tracing.Tracing.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//该类为切面
public class TracingAspect {

    private static final Logger LOG = LoggerFactory.getLogger(TracingAspect.class);

    public void around(final ProceedingJoinPoint pjp) throws Throwable {
        final Object[] args = pjp.getArgs();
        if (args.length > 0 && args[0] instanceof Tracing) {
            final Tracing tracing = (Tracing) args[0];
            try (final Scope scope = tracing.activate()) {
                final Object ret = pjp.proceed();
                LOG.info("pjp.proceed() within tracing's return value is {}", ret);
            }
        } else {
            final Object ret = pjp.proceed();
            LOG.info("pjp.proceed() without tracing's return value is {}", ret);
        }
    }
}
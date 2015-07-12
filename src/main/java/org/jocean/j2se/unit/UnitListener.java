package org.jocean.j2se.unit;

import org.springframework.context.ConfigurableApplicationContext;

public interface UnitListener {

    public void postUnitCreated(final String unitPath, final ConfigurableApplicationContext appctx);

    public void beforeUnitClosed(final String unitPath, final ConfigurableApplicationContext appctx);
}

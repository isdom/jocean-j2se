package org.jocean.j2se.unit;

import org.springframework.context.ConfigurableApplicationContext;

public interface UnitListener {

    public void onUnitCreated(final ConfigurableApplicationContext appctx);

    public void onUnitClosed(final ConfigurableApplicationContext appctx);
}

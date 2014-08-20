package org.jocean.ext.commons.redis;

public interface JedisStatisticsMXBean {

    long getCacheHits();

    long getCacheMisses();
}

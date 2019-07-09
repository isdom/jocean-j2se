package org.jocean.j2se.netty;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.netty.buffer.PooledByteBufAllocatorMetric;

public class NettyPooledAllocatorMetrics implements MeterBinder {

    public NettyPooledAllocatorMetrics(final PooledByteBufAllocatorMetric pooledAllocatorMetric) {
        this._pooledByteBufAllocatorMetric = pooledAllocatorMetric;
    }

    @Override
    public void bindTo(final MeterRegistry registry) {
        Gauge.builder("netty.pooled.active.memory", this._pooledByteBufAllocatorMetric,
                metric -> PooledAllocatorStats.getActiveAllocationsInBytes(metric))
            .baseUnit(BaseUnits.BYTES)
            .description("The active memory size of this netty allocator") // optional
            .register(registry);

        Gauge.builder("netty.pooled.used.memory", this._pooledByteBufAllocatorMetric,
                metric -> metric.usedDirectMemory())
            .tags("type", "direct")
            .baseUnit(BaseUnits.BYTES)
            .description("The used memory of this netty allocator") // optional
            .register(registry);

        Gauge.builder("netty.pooled.used.memory", this._pooledByteBufAllocatorMetric,
                metric -> metric.usedHeapMemory())
            .tags("type", "heap")
            .baseUnit(BaseUnits.BYTES)
            .description("The used memory of this netty allocator") // optional
            .register(registry);

        Gauge.builder("netty.pooled.arenas.num", this._pooledByteBufAllocatorMetric,
                metric -> metric.numDirectArenas())
            .tags("type", "direct")
            .description("The arenas number used by this netty allocator") // optional
            .register(registry);

        Gauge.builder("netty.pooled.arenas.num", this._pooledByteBufAllocatorMetric,
                metric -> metric.numHeapArenas())
            .tags("type", "heap")
            .description("The arenas number used by this netty allocator") // optional
            .register(registry);

        Gauge.builder("netty.pooled.cachesize", this._pooledByteBufAllocatorMetric,
                metric -> metric.tinyCacheSize())
            .tags("type", "tiny")
            .description("The cachesize used by this netty allocator") // optional
            .register(registry);

        Gauge.builder("netty.pooled.cachesize", this._pooledByteBufAllocatorMetric,
                metric -> metric.smallCacheSize())
            .tags("type", "small")
            .description("The cachesize used by this netty allocator") // optional
            .register(registry);

        Gauge.builder("netty.pooled.cachesize", this._pooledByteBufAllocatorMetric,
                metric -> metric.normalCacheSize())
            .tags("type", "normal")
            .description("The cachesize used by this netty allocator") // optional
            .register(registry);

        Gauge.builder("netty.pooled.threadlocalcache.num", this._pooledByteBufAllocatorMetric,
                metric -> metric.numThreadLocalCaches())
            .description("The number of thread local caches used by this netty allocator") // optional
            .register(registry);

        Gauge.builder("netty.pooled.chunk.size", this._pooledByteBufAllocatorMetric,
                metric -> metric.chunkSize())
            .description("The arena chunk size of this netty allocator") // optional
            .register(registry);
    }

    private final PooledByteBufAllocatorMetric _pooledByteBufAllocatorMetric;
}

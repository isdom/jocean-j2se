package org.jocean.j2se.netty;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocatorMetric;

public class NettyPooledAllocatorMetrics implements MeterBinder {

    public NettyPooledAllocatorMetrics(final PooledByteBufAllocatorMetric pooledAllocatorMetric) {
        this._pooledByteBufAllocatorMetric = pooledAllocatorMetric;
    }

    @Override
    public void bindTo(final MeterRegistry registry) {
        Gauge.builder("netty.pooled.active.memory", this._pooledByteBufAllocatorMetric,
                metric -> PooledAllocatorStats.getActiveAllocationsInBytes(metric))
            .description("The active memory size of this netty allocator") // optional
            .baseUnit(BaseUnits.BYTES)
            .register(registry);

        Gauge.builder("netty.pooled.used.memory", this._pooledByteBufAllocatorMetric,
                metric -> metric.usedDirectMemory())
            .tags("type", "direct")
            .description("The used memory of this netty allocator") // optional
            .baseUnit(BaseUnits.BYTES)
            .register(registry);

        Gauge.builder("netty.pooled.used.memory", this._pooledByteBufAllocatorMetric,
                metric -> metric.usedHeapMemory())
            .tags("type", "heap")
            .description("The used memory of this netty allocator") // optional
            .baseUnit(BaseUnits.BYTES)
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
            .baseUnit(BaseUnits.BYTES)
            .register(registry);

        {
            int idx = 0;
            for (final PoolArenaMetric poolArenaMetric : _pooledByteBufAllocatorMetric.directArenas()) {
                metricsOfPoolArena(registry, poolArenaMetric, idx++, "direct");
            }
        }
        {
            int idx = 0;
            for (final PoolArenaMetric poolArenaMetric : _pooledByteBufAllocatorMetric.heapArenas()) {
                metricsOfPoolArena(registry, poolArenaMetric, idx++, "heap");
            }
        }
    }

    private void metricsOfPoolArena(final MeterRegistry registry, final PoolArenaMetric poolArenaMetric, final int idx, final String type) {
        /**
         * the number of thread caches backed by this arena.
         */
        Gauge.builder("netty.pooled.arena.threadcaches.num", poolArenaMetric,
                metric -> metric.numThreadCaches())
            .tags("type", type, "index", Integer.toString(idx))
            .description("The number of thread caches backed by this arena") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.total_allocations.num", poolArenaMetric,
                metric -> metric.numAllocations())
            .tags("type", type, "index", Integer.toString(idx))
            .description("The number of allocations done via the arena. This includes all sizes") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.allocations.num", poolArenaMetric,
                metric -> metric.numTinyAllocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "tiny")
            .description("The number of special size allocations done via the arena") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.allocations.num", poolArenaMetric,
                metric -> metric.numSmallAllocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "small")
            .description("The number of special size allocations done via the arena") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.allocations.num", poolArenaMetric,
                metric -> metric.numNormalAllocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "normal")
            .description("The number of special size allocations done via the arena") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.allocations.num", poolArenaMetric,
                metric -> metric.numHugeAllocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "huge")
            .description("The number of special size allocations done via the arena") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.total_deallocations.num", poolArenaMetric,
                metric -> metric.numDeallocations())
            .tags("type", type, "index", Integer.toString(idx))
            .description("The number of deallocations done via the arena. This includes all sizes") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.deallocations.num", poolArenaMetric,
                metric -> metric.numTinyDeallocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "tiny")
            .description("The number of special size deallocations done via the arena") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.deallocations.num", poolArenaMetric,
                metric -> metric.numSmallDeallocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "small")
            .description("The number of special size deallocations done via the arena") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.deallocations.num", poolArenaMetric,
                metric -> metric.numNormalDeallocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "normal")
            .description("The number of special size deallocations done via the arena") // optional
            .register(registry);

        FunctionCounter.builder("netty.pooled.arena.deallocations.num", poolArenaMetric,
                metric -> metric.numHugeDeallocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "huge")
            .description("The number of special size deallocations done via the arena") // optional
            .register(registry);

        Gauge.builder("netty.pooled.arena.active.total_allocations", poolArenaMetric,
                metric -> metric.numActiveAllocations())
            .tags("type", type, "index", Integer.toString(idx))
            .description("The number of currently active allocations") // optional
            .register(registry);

        Gauge.builder("netty.pooled.arena.active.allocations", poolArenaMetric,
                metric -> metric.numActiveTinyAllocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "tiny")
            .description("The number of currently active special size allocations") // optional
            .register(registry);

        Gauge.builder("netty.pooled.arena.active.allocations", poolArenaMetric,
                metric -> metric.numActiveSmallAllocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "small")
            .description("The number of currently active special size allocations") // optional
            .register(registry);

        Gauge.builder("netty.pooled.arena.active.allocations", poolArenaMetric,
                metric -> metric.numActiveNormalAllocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "normal")
            .description("The number of currently active special size allocations") // optional
            .register(registry);

        Gauge.builder("netty.pooled.arena.active.allocations", poolArenaMetric,
                metric -> metric.numActiveHugeAllocations())
            .tags("type", type, "index", Integer.toString(idx), "size", "huge")
            .description("The number of currently active special size allocations") // optional
            .register(registry);

        Gauge.builder("netty.pooled.arena.active.allocated", poolArenaMetric,
                metric -> metric.numActiveBytes())
            .tags("type", type, "index", Integer.toString(idx))
            .description("The number of active bytes that are currently allocated by the arena") // optional
            .baseUnit(BaseUnits.BYTES)
            .register(registry);

        Gauge.builder("netty.pooled.arena.chunk.num", poolArenaMetric,
                metric -> metric.numChunkLists())
            .tags("type", type, "index", Integer.toString(idx))
            .description("The number of chunk lists for the arena") // optional
            .register(registry);

        /**
         * Returns an unmodifiable {@link List} which holds {@link PoolChunkListMetric}s.
         */
//        metrics.put("6_1_chunkLists", metricsOfChunkLists(poolArenaMetric.chunkLists()));

        Gauge.builder("netty.pooled.arena.subpages.num", poolArenaMetric,
                metric -> metric.numTinySubpages())
            .tags("type", type, "index", Integer.toString(idx), "size", "tiny")
            .description("The number of special size sub-pages for the arena") // optional
            .register(registry);

        /**
         * Returns an unmodifiable {@link List} which holds {@link PoolSubpageMetric}s for tiny sub-pages.
         */
//        metrics.put("7_1_tinySubpages", metricsOfSubpages(
//                poolArenaMetric.numTinySubpages(),
//                poolArenaMetric.tinySubpages()));

        Gauge.builder("netty.pooled.arena.subpages.num", poolArenaMetric,
                metric -> metric.numSmallSubpages())
            .tags("type", type, "index", Integer.toString(idx), "size", "small")
            .description("The number of special size sub-pages for the arena") // optional
            .register(registry);

        /**
         * Returns an unmodifiable {@link List} which holds {@link PoolSubpageMetric}s for small sub-pages.
         */
//        metrics.put("8_1_smallSubpage", metricsOfSubpages(
//                poolArenaMetric.numSmallSubpages(),
//                poolArenaMetric.smallSubpages()));
    }

    private final PooledByteBufAllocatorMetric _pooledByteBufAllocatorMetric;
}

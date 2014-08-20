package io.netty.handler.traffic;

import io.netty.channel.ChannelHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 统计字节流,AbstractTrafficShapingHandler的默认实现貌似有一堆问题,后续版本需要持续关注
 */
@ChannelHandler.Sharable
public class GlobalByteTrafficMonitor extends AbstractTrafficShapingHandler {

    /**
     * Create the global TrafficCounter
     */
    void createGlobalTrafficCounter(ScheduledExecutorService executor) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        TrafficCounterExt tc = new TrafficCounterExt(this, executor, "GlobalTC",
                checkInterval);
        setTrafficCounter(tc);
        tc.start();
    }

    public GlobalByteTrafficMonitor() {
        createGlobalTrafficCounter(Executors.newSingleThreadScheduledExecutor());
    }

    public TrafficCounter getTrafficCounter() {
        return trafficCounter;
    }
}

package org.jocean.ext.netty.initializer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.traffic.TrafficCounterExt;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.jocean.ext.netty.handler.CloseOnIdleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.concurrent.TimeUnit;

/**
 * 提供日志功能和统计功能
 */
public abstract class BaseInitializer extends ChannelInitializer implements ApplicationContextAware {
    //放在最顶上，以让NETTY默认使用SLF4J
    static {
        if (!(InternalLoggerFactory.getDefaultFactory() instanceof Slf4JLoggerFactory)) {
            InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(BaseInitializer.class);
    private TrafficCounterExt trafficCounter;
    private static final LoggingHandler LOGGING_HANDLER = new LoggingHandler();

    private boolean logByteStream;
    private boolean logMessage = true;
    private int idleTimeSeconds = 0; // in seconds

    protected ApplicationContext applicationContext;


    @Override
    public void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (logByteStream) {
            logger.debug("ByteLogger enabled");
            pipeline.addLast("byteLogger", LOGGING_HANDLER);
        }
        if (idleTimeSeconds > 0) {
            pipeline.addLast("idleHandler", new CloseOnIdleHandler(0, 0, idleTimeSeconds, TimeUnit.SECONDS));
        }
        if (trafficCounter != null) {
            pipeline.addLast("globalByteTrafficMonitor", trafficCounter.getByteTrafficMonitor());
        }
        addCodecHandler(pipeline);
        if (trafficCounter != null) {
            pipeline.addLast("globalMessageTrafficMonitor", trafficCounter.getMessageTrafficMonitor());
        }
        if (logMessage) {
            logger.debug("MessageLogger enabled");
            pipeline.addLast("messageLogger", LOGGING_HANDLER);
        }
        addBusinessHandler(pipeline);
    }

    /**
     * 增加编解码handler
     *
     * @param pipeline
     * @throws Exception
     */
    protected abstract void addCodecHandler(ChannelPipeline pipeline) throws Exception;

    /**
     * 增加负责处理具体业务逻辑的handler
     *
     * @param pipeline
     * @throws Exception
     */
    protected abstract void addBusinessHandler(ChannelPipeline pipeline) throws Exception;

    public boolean isLogByteStream() {
        return logByteStream;
    }

    /**
     * 是否提供记录二进制日志功能
     *
     * @param logByteStream
     */
    public void setLogByteStream(boolean logByteStream) {
        this.logByteStream = logByteStream;
    }

    public boolean isLogMessage() {
        return logMessage;
    }

    /**
     * 是否提供记录解码后的数据包日志功能
     *
     * @param logMessage
     */
    public void setLogMessage(boolean logMessage) {
        this.logMessage = logMessage;
    }

    public TrafficCounterExt getTrafficCounter() {
        return trafficCounter;
    }

    public void setTrafficCounter(TrafficCounterExt trafficCounter) {
        this.trafficCounter = trafficCounter;
    }

    public int getIdleTimeSeconds() {
        return idleTimeSeconds;
    }

    /**
     * 最大连接空闲时间，如果设置的值大于0，则空闲该时间后将关闭连接
     *
     * @param idleTimeSeconds 单位 秒
     */
    public void setIdleTimeSeconds(int idleTimeSeconds) {
        this.idleTimeSeconds = idleTimeSeconds;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

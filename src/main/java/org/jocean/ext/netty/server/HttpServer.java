package org.jocean.ext.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jocean.ext.netty.initializer.HttpServerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.MBeanExporter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private static final int MAX_RETRY = 20;
    private static final long RETRY_TIMEOUT = 30 * 1000;   // 30s


    private ServerBootstrap bootstrap = new ServerBootstrap();
    private String acceptIp = "0.0.0.0";
    private int acceptPort = 80;
    private int dumpSaipBytes = 256;
    private Map<String, String> copyProperties = new HashMap<String, String>();

    private Channel channel;
    private HttpServerInitializer pipelineFactory;

    //	JMX support
    private volatile MBeanExporter mbeanExporter = null;

    /**
     * @throws java.io.IOException
     */
    public void start() throws IOException {
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(pipelineFactory)
                .localAddress(this.acceptIp, this.acceptPort)
                .option(ChannelOption.SO_BACKLOG, 10240)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_RCVBUF, 8 * 1024)  //不宜太小,否则会有性能问题,默认为8K,超过64K时需要在bind前设置
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_LINGER, -1);

        int retryCount = 0;
        boolean binded = false;

        do {
            try {
                channel = bootstrap.bind().channel();
                binded = true;
            } catch (ChannelException e) {
                logger.warn("start failed : " + e + ", and retry...");

                //  对绑定异常再次进行尝试
                retryCount++;
                if (retryCount >= MAX_RETRY) {
                    //  超过最大尝试次数
                    throw e;
                }
                try {
                    Thread.sleep(RETRY_TIMEOUT);
                } catch (InterruptedException ignored) {
                }
            }
        } while (!binded);
    }

    public void stop() {
        if (null != channel) {
            channel.disconnect();
            channel = null;
        }
    }


    /**
     * @param acceptPort the acceptPort to set
     */
    public void setAcceptPort(int acceptPort) {
        this.acceptPort = acceptPort;
    }

    /**
     * @param acceptIp the acceptIp to set
     */
    public void setAcceptIp(String acceptIp) {
        this.acceptIp = acceptIp;
    }

    /**
     * @param dumpSaipBytes the dumpSaipBytes to set
     */
    public void setDumpSaipBytes(int dumpSaipBytes) {
        this.dumpSaipBytes = dumpSaipBytes;
    }

    public String getAcceptIp() {
        return acceptIp;
    }

    public int getAcceptPort() {
        return acceptPort;
    }

    public int getDumpSaipBytes() {
        return dumpSaipBytes;
    }

    public Map<String, String> getCopyProperties() {
        return Collections.unmodifiableMap(copyProperties);
    }

    public void setCopyProperties(Map<String, String> copyProperties) {
        this.copyProperties.clear();

        for (Map.Entry<String, String> entry : copyProperties.entrySet()) {
            if (null != entry.getValue()) {
                this.copyProperties.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * @return the mbeanExporter
     */
    public MBeanExporter getMbeanExporter() {
        return mbeanExporter;
    }

    /**
     * @param mbeanExporter the mbeanExporter to set
     */
    public void setMbeanExporter(MBeanExporter mbeanExporter) {
        this.mbeanExporter = mbeanExporter;
    }

    public void setPipelineFactory(HttpServerInitializer pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }
}

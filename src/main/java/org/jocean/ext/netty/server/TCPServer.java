package org.jocean.ext.netty.server;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.jocean.ext.netty.initializer.TcpServerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class TCPServer {
    private static final Logger logger = LoggerFactory.getLogger(TCPServer.class);
    private static final int MAX_RETRY = 20;
    private static final long RETRY_TIMEOUT = 30 * 1000;   // 30s

    private String acceptIp = "0.0.0.0";
    private int acceptPort = 8888;

    @Value("${zk.address:}")
    private String zkAddress;
    private String serviceCode;
    private Map<String, Object> zkNodeData = new HashMap<>();

    private Channel channel;
    private TcpServerInitializer pipelineFactory;
    ServerBootstrap bootstrap = new ServerBootstrap();

    CuratorFramework zkClient;

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
                channel = bootstrap.bind().syncUninterruptibly().channel();
                binded = true;
                if (StringUtils.isNotBlank(zkAddress)) {
                    zkClient = CuratorFrameworkFactory.newClient(zkAddress, new ExponentialBackoffRetry(1000, 5));
                    zkClient.start();

                    try {
                        final String path = "/jocean.servers/tcp/" + (StringUtils.isNotBlank(serviceCode) ? serviceCode : System.getProperty("app.name"))
                                + "/" + InetAddress.getLocalHost().getHostAddress() + ":" + acceptPort;
                        try {
                            zkClient.delete().forPath(path);
                        } catch (Exception ignored) {
                        }

                        String jmxPort = System.getProperty("com.sun.management.jmxremote.port");
                        if (jmxPort != null) {
                            zkNodeData.put("jmxPort", jmxPort);
                        }
                        zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, JSON.toJSONBytes(zkNodeData));

                        //zk重新连上后要把自己重新注册上去
                        zkClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                            @Override
                            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                                if (newState.equals(ConnectionState.RECONNECTED)) {
                                    try {
                                        zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, JSON.toJSONBytes(zkNodeData));
                                    } catch (Exception e) {
                                        logger.error("reCreate zk node error", e);
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        logger.error("create zk node error", e);
                    }
                }
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

        logger.info("start succeed in " + acceptIp + ":" + acceptPort);
    }

    public void stop() {
        //channel停止监听后zk再断开
        if (null != channel) {
            channel.disconnect().syncUninterruptibly();
            channel = null;
        }
        if (null != zkClient) {
            zkClient.close();
        }
    }

    public String getAcceptIp() {
        return acceptIp;
    }

    public void setAcceptIp(String acceptIp) {
        this.acceptIp = acceptIp;
    }

    public int getAcceptPort() {
        return acceptPort;
    }

    public void setAcceptPort(int acceptPort) {
        this.acceptPort = acceptPort;
    }

    public void setPipelineFactory(TcpServerInitializer pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }

    public String getZkAddress() {
        return zkAddress;
    }

    public void setZkAddress(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public void setExtraZkNodeData(Map<String, Object> extraData) {
        zkNodeData.putAll(extraData);
    }
}

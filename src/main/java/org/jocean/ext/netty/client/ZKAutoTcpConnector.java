package org.jocean.ext.netty.client;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;
import org.jocean.ext.jmx.PrefixableMBeanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class ZKAutoTcpConnector {
    private static final Logger logger = LoggerFactory.getLogger(ZKAutoTcpConnector.class);
    private static final long RETRY_TIMEOUT = 10 * 1000;   // 10s
    private static final String zkNodePrefix = "/jocean.servers/tcp/";

    @Autowired
    private PrefixableMBeanExporter mBeanExporter;

    @Value("${zk.address}")
    private String zkAddress;
    private String remoteServiceCode;
    @Autowired
    private TcpMessageSender messageSender;
    @Autowired
    private LinkedBlockingQueue sendQueue;

    CuratorFramework zkClient;
    Map<String, TCPConnector> connectors = new ConcurrentHashMap<>();
    Timer timer = new Timer();

    public void start() {
        if (zkClient == null) {
            zkClient = CuratorFrameworkFactory.newClient(zkAddress, new ExponentialBackoffRetry(1000, 3));
            zkClient.start();
        }
        final String servicePath = zkNodePrefix + remoteServiceCode;

        try {
            for (String serverInfo : zkClient.getChildren().forPath(servicePath)) {
                if (!StringUtils.contains(serverInfo, ":")) {
                    logger.error("{} is invalid host,ignore!", serverInfo);
                    continue;
                }
                openConnection(serverInfo);
            }
        } catch (KeeperException.NoNodeException noNodeException) {
            logger.error("can not find remote service with code {},wait and retry", remoteServiceCode);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    start();
                }
            }, RETRY_TIMEOUT);
        } catch (Exception e) {
            logger.error("", e);
        }

        final PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                String path = event.getData().getPath();
                String service = path.replaceAll(servicePath + "/", "");

                synchronized (ZKAutoTcpConnector.this) {
                    switch (event.getType()) {
                        case CHILD_ADDED:
                            openConnection(service);
                            break;
                        case CHILD_REMOVED:
                            TCPConnector connector = connectors.get(service);
                            if (connector != null && !connector.isActive())
                                closeConnection(service);
                    }
                }
            }
        });
        try {
            pathChildrenCache.start();
        } catch (Exception e) {
            logger.error("listen zk service path error", e);
        }
    }

    protected void openConnection(String hostAddress) {
        if (connectors.containsKey(hostAddress))
            return;
        String[] host = hostAddress.split(":");
        String ip = host[0];
        int port = Integer.parseInt(host[1]);
        TCPConnector connector = getTCPConnector();
        connector.setDestIp(ip);
        connector.setDestPort(port);
        connector.start();

        connectors.put(hostAddress, connector);

        try {
            byte[] bytes = zkClient.getData().forPath(zkNodePrefix + remoteServiceCode + "/" + hostAddress);
            if (bytes != null) {
                JSONObject data = JSON.parseObject(new String(bytes));
                JSONArray paths = data.getJSONArray("paths");
                if (paths != null) {
                    for (Object path : paths) {
                        messageSender.addSender(path.toString(), sendQueue);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }

        try {
            mBeanExporter.registerManagedResource(connector, new ObjectName("prefix:class=transport.tcp.connector,group=" + remoteServiceCode
                    + ",ip=" + ip + "_" + port + ",name=outbound"));

            mBeanExporter.registerManagedResource(connector.getTcpClientInitializer().getTrafficCounter()
                    , new ObjectName("prefix:class=transport.tcp.connector,group=" + remoteServiceCode
                    + ",ip=" + ip + "_" + port + ",name=statisticor"));
        } catch (MalformedObjectNameException e) {
            logger.error("register jmx error", e);
        }
        logger.info("TCP connection {} opened", hostAddress);
    }

    protected void closeConnection(String hostAddress) {
        TCPConnector connector = connectors.remove(hostAddress);
        if (connector == null)
            return;
        logger.info("close TCP connection {}", hostAddress);
        connector.stop();
        try {
            mBeanExporter.unregisterManagedResource(new ObjectName("prefix:class=transport.tcp.connector,group=" + remoteServiceCode
                    + ",ip=" + connector.getDestIp() + "_" + connector.getDestPort() + ",name=outbound"));

            mBeanExporter.unregisterManagedResource(new ObjectName("prefix:class=transport.tcp.connector,group=" + remoteServiceCode
                    + ",ip=" + connector.getDestIp() + "_" + connector.getDestPort() + ",name=statisticor"));
        } catch (MalformedObjectNameException e) {
            logger.error("unregisterManagedResource error", e);
        }
    }

    public void stop() {
        if (null != zkClient) {
            zkClient.close();
            zkClient = null;
        }
        for (String hostAddress : connectors.keySet()) {
            closeConnection(hostAddress);
        }
    }

    public String getRemoteServiceCode() {
        return remoteServiceCode;
    }

    public void setRemoteServiceCode(String remoteServiceCode) {
        this.remoteServiceCode = remoteServiceCode;
    }

    public abstract TCPConnector getTCPConnector();
}

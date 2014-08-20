package org.jocean.ext.netty.client;

import org.apache.commons.lang3.StringUtils;
import org.jocean.ext.transport.Sender;
import org.jocean.ext.transport.protocol.pdu.PDUBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class TcpMessageSender implements Sender<PDUBean> {
    private static final Logger logger = LoggerFactory.getLogger(TcpMessageSender.class);

    private volatile static TcpMessageSender instance;
    private ConcurrentHashMap<String, Queue> messageSenderMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Queue> messageRegexSenderMap = new ConcurrentHashMap<>();

    private TcpMessageSender() {
    }

    public static TcpMessageSender getInstance() {
        if (instance == null) {
            synchronized (TcpMessageSender.class) {
                if (instance == null) {
                    instance = new TcpMessageSender();
                }
            }
        }
        return instance;
    }

    public boolean send(PDUBean message) {
        String messageCode = message.getPath();
        Queue queue = messageSenderMap.get(messageCode);
        if (queue == null) {
            logger.debug("can't match messageCode {},lookup by regex", messageCode);
            if (!messageCode.endsWith("/")) {
                messageCode += "/";
            }
            for (String key : messageRegexSenderMap.keySet()) {
                if (messageCode.matches(key) || (messageCode + "/").matches(key)) { //  /channel/rose/category/1 和 /channel/rose/category 都可以匹配  /channel/rose/category/{categoryid}
                    queue = messageRegexSenderMap.get(key);
                    break;
                }
            }
            if (queue == null) {
                logger.error("can't find sender of {}", message);
                return false;
            }
        }
        return queue.add(message);
    }

    public void addSender(String messageCode, Queue sender) {
        if (messageCode.contains("{") && messageCode.contains("}")) {
            messageCode = getRegexMessageCode(messageCode);
            messageRegexSenderMap.putIfAbsent(messageCode, sender);
        } else {
            messageSenderMap.putIfAbsent(messageCode, sender);
        }
    }

    public void removeSender(String messageCode) {
        messageSenderMap.remove(messageCode);
        messageRegexSenderMap.remove(getRegexMessageCode(messageCode));
    }

    private String getRegexMessageCode(String messageCode) {
        if (!messageCode.endsWith("/")) {
            messageCode += "/";
        }
        messageCode = StringUtils.join(messageCode.split("\\{.*?\\}"), "[^/]*?");
        return messageCode;
    }

    public ConcurrentHashMap<String, Queue> getMessageSenderMap() {
        return messageSenderMap;
    }
}

package org.jocean.ext.commons.log.bean;

import org.jocean.ext.netty.handler.codec.tcp.TcpRequest;

public class ServerBizLog extends TcpRequest {

    private Object logBean;

    public ServerBizLog() {
    }

    public ServerBizLog(Object logBean) {
        this.logBean = logBean;
    }

    public Object getLogBean() {
        return logBean;
    }

    public void setLogBean(Object logBean) {
        this.logBean = logBean;
    }

    @Override
    public String getPath() {
        return "/channel/rose/log/serverBizLog";
    }

}

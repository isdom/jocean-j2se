package org.jocean.ext.commons.log.bean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jocean.ext.netty.handler.codec.tcp.TcpRequest;

public class ServerBizLog extends TcpRequest {

    private Object logBean;

    public ServerBizLog() {
    }

    public ServerBizLog(Object logBean) {
        this.logBean = logBean;
    }

    public ServerBizLog(Object logBean, Object terminalPropertyBean, Class terminalPropertyBeanType) {
        //去掉多余的信息,只保留终端属性
        JSONObject bean = (JSONObject) JSON.toJSON(JSON.parseObject(JSON.toJSONString(terminalPropertyBean), terminalPropertyBeanType));
        if (bean != null)
            bean.putAll((JSONObject) JSON.toJSON(logBean));
        this.logBean = bean;
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

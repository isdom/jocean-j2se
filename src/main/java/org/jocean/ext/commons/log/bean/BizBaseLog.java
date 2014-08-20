package org.jocean.ext.commons.log.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Date;

public class BizBaseLog {
    @JSONField(name = "tableName")
    private String tableName;
    @JSONField(name = "tableSuffix")
    private String tableSuffix;
    @JSONField(name = "create_time")
    private Date createTime = new Date();
    @JSONField(name = "response_code")
    private int responseCode = 200;
    @JSONField(name = "exception_desc")
    private String exceptionDesc;
    private String clientIp;

    public BizBaseLog(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableSuffix() {
        return tableSuffix;
    }

    public void setTableSuffix(String tableSuffix) {
        this.tableSuffix = tableSuffix;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getExceptionDesc() {
        return exceptionDesc;
    }

    public void setExceptionDesc(String exceptionDesc) {
        this.exceptionDesc = exceptionDesc;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
}

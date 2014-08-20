package org.jocean.ext.transport.protocol.pdu;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jocean.ext.netty.handler.codec.tcp.KVHeader;

import java.util.List;
import java.util.UUID;

//0               1               2               3
//0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
//+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//|  MagicCode(1) |  BasicVer(1)  |       HeaderLength(2)         |
//+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//|                         BodyLength(4)                         |
//+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//|                                                               |
//|                       transaction(16)                         |
//|                                                               |
//|                                                               |
//+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//|         code length(2)        |                               |
//|         code field(...)                                       |
//|     JSON header length(2)     |                               |
//|   JSON header field(...)                                      |
//|   application data field(...)                                 |
//|                                                               |
//|                                                               |
//+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

//字段名称    字段大小    取值范围    功能说明
//协议标识
//(MagicCode) 1(byte) {0x18}
//基础协议版本
//(BasicVer) 1(byte) [1,255] 标识基础协议的版本号，不会频繁更新，以1为开始版本 json(1)
//协议头长度
//(HeaderLength)  2(byte)   [32,65535]   整个协议头长度，最小值为协议头的固定部分大小(28)，高位字节序
//包体长度
//(BodyLength)  4(byte)   [0,4294967296]   正文包长度
//事务标识
//(transaction)   16(byte)  GUID    参照GUID的生成算法，由请求或者通知的发送者保证其在任何时间、地点、平台唯一，请求和响应消息的事物标识必须一致。该标识用于保证请求和响应的唯一对应以及消息的不重复性
//消息编码长度
//(code length) 2(byte) [1,65535] 消息编码的长度，高位字节序
//消息编码数据域
//(code field)    n   n/a 唯一的一个消息编码，长度为code length
//消息JSON格式头数据长度
//(jsonHeaderLength) 2(byte) [1,65535] 消息JSON格式头数据的长度，高位字节序
//消息JSON格式头数据域
//(JSON header field)    n   n/a 数据来源例如：http头中的数据、dispatcher中增加的数据，长度为jsonHeaderLength
//协议包体数据域
//(data field)    n   n/a 协议包体数据，长度为 BodyLength

public class PDUHeader {
    public static final int MIN_XIP_HEADER_LENGTH = 28;

    // (protocol id)  1(byte) {0x88}
    public static final byte MAGIC_CODE = 0x18;

    private byte magicCode = MAGIC_CODE;
    private byte basicVer = 1;
    private short headerLength;
    private int bodyLength;
    private long firstTransaction;
    private long secondTransaction;
    private short codeLength;
    private String messageCode;
    private short jsonHeaderLength;
    private List<KVHeader> jsonHeader;

    public boolean isMagicCodeValid() {
        return (MAGIC_CODE == this.magicCode);
    }

    public byte getMagicCode() {
        return magicCode;
    }

    public void setMagicCode(byte magicCode) {
        this.magicCode = magicCode;
    }

    /**
     * @return the basicVer
     */
    public byte getBasicVer() {
        return basicVer;
    }

    /**
     * @param ver the basicVer to set
     */
    public void setBasicVer(byte ver) {
        basicVer = ver;
    }

    public short getHeaderLength() {
        return headerLength;
    }

    public void setHeaderLength(short headerLength) {
        this.headerLength = headerLength;
    }

    /**
     * @return the firstTransaction
     */
    public long getFirstTransaction() {
        return firstTransaction;
    }

    /**
     * @param firstTransaction the firstTransaction to set
     */
    public void setFirstTransaction(long firstTransaction) {
        this.firstTransaction = firstTransaction;
    }

    /**
     * @return the secondTransaction
     */
    public long getSecondTransaction() {
        return secondTransaction;
    }

    /**
     * @param secondTransaction the secondTransaction to set
     */
    public void setSecondTransaction(long secondTransaction) {
        this.secondTransaction = secondTransaction;
    }

    public short getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(short codeLength) {
        this.codeLength = codeLength;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public List<KVHeader> getJsonHeader() {
        return jsonHeader;
    }

    public void setJsonHeader(List<KVHeader> jsonHeader) {
        this.jsonHeader = jsonHeader;
    }

    public short getJsonHeaderLength() {
        return jsonHeaderLength;
    }

    public void setJsonHeaderLength(short jsonHeaderLength) {
        this.jsonHeaderLength = jsonHeaderLength;
    }

    public void setTransaction(UUID uuid) {
        this.firstTransaction = uuid.getMostSignificantBits();
        this.secondTransaction = uuid.getLeastSignificantBits();
    }

    public UUID getTransactionAsUUID() {
        return new UUID(this.firstTransaction, this.secondTransaction);
    }

    public String toString() {

        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

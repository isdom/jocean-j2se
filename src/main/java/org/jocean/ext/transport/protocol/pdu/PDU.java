package org.jocean.ext.transport.protocol.pdu;

/**
 * Protocol Data Unit 协议数据单元
 */
public class PDU {

    private PDUHeader header;
    private byte[] body;

    public PDUHeader getHeader() {
        return header;
    }

    public void setHeader(PDUHeader header) {
        this.header = header;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}

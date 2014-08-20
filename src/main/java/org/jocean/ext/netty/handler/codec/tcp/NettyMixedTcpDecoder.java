package org.jocean.ext.netty.handler.codec.tcp;

import com.alibaba.fastjson.JSON;
import com.google.common.primitives.Shorts;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import org.jocean.ext.Constants;
import org.jocean.ext.netty.handler.codec.http.HttpRequestExt;
import org.jocean.ext.netty.handler.codec.http.HttpResponseExt;
import org.jocean.ext.transport.protocol.pdu.PDUHeader;
import org.jocean.ext.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 非线程安全
 */
public class NettyMixedTcpDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(NettyMixedTcpDecoder.class);

    private static final AttributeKey<PDUHeader> XIP_HEADER_ATTRIBUTE_KEY = AttributeKey.valueOf("XIP_HEADER_ATTRIBUTE_KEY");

    //大于1M的数据包可能是问题数据包
    private int maxMessageLength = 1024 * 1024;

    private int dumpBytes = 256;

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        PDUHeader header = ctx.attr(XIP_HEADER_ATTRIBUTE_KEY).get();
        int minHeaderSize = PDUHeader.MIN_XIP_HEADER_LENGTH;
        if (null == header) {
            if (buffer.readableBytes() < PDUHeader.MIN_XIP_HEADER_LENGTH) {
                return;
            }
            byte[] bytes = new byte[2];
            buffer.getBytes(2, bytes);
            if (buffer.readableBytes() < Shorts.fromByteArray(bytes)) { //不够完整头大小
                return;
            }

            logger.debug("parse header... try parse...");

            byte magicCode = buffer.readByte();
            if (PDUHeader.MAGIC_CODE != magicCode) {
                ctx.channel().close().await();
                throw new RuntimeException("wrong magicCode:" + magicCode);
            }

            header = new PDUHeader();
            header.setBasicVer(buffer.readByte());
            header.setHeaderLength(buffer.readShort());
            header.setBodyLength(buffer.readInt());

            header.setFirstTransaction(buffer.readLong());
            header.setSecondTransaction(buffer.readLong());

            //read message code
            header.setCodeLength(buffer.readShort());
            bytes = new byte[header.getCodeLength()];
            buffer.readBytes(bytes);
            header.setMessageCode(new String(bytes));
            //read json header
            header.setJsonHeaderLength(buffer.readShort());
            bytes = new byte[header.getJsonHeaderLength()];
            buffer.readBytes(bytes);
            header.setJsonHeader(JSON.parseArray(new String(bytes), KVHeader.class));

            int leftHeaders = header.getHeaderLength() - buffer.readerIndex();
            if (leftHeaders > 0) {//丢弃不识别的头内容
                logger.warn("skip {} bytes unknown header", leftHeaders);
                buffer.skipBytes(leftHeaders);
            }

            logger.debug("header-->" + header);

            if (header.getBodyLength() < 0 || (maxMessageLength > 0 && header.getBodyLength() > maxMessageLength)) {
                byte[] headerBytes = new byte[minHeaderSize];
                buffer.getBytes(0, headerBytes);
                logger.error("header.messageLength ({}) error,must between 0 and {}, so drop this connection {}.\r\ndump bytes received:\r\n{}"
                        , header.getBodyLength(), maxMessageLength, ctx.channel(), ByteUtils.bytesAsHexString(headerBytes, dumpBytes));
                ctx.channel().close().await();
                throw new RuntimeException("messageLength error:" + header.getBodyLength());
            }

            ctx.attr(XIP_HEADER_ATTRIBUTE_KEY).set(header);
        }

        int bodyLength = header.getBodyLength();
        if (buffer.readableBytes() < bodyLength) {
            if (logger.isDebugEnabled()) {
                logger.debug("readableBytes {} smaller than packageLength {},waiting for remain bytes", buffer.readableBytes(), bodyLength);
            }
            return;
        }

        //为下一次在同一ctx上进行xip接受初始化环境
        ctx.attr(XIP_HEADER_ATTRIBUTE_KEY).remove();

        byte[] bytes = new byte[bodyLength];
        buffer.readBytes(bytes);

        if (logger.isTraceEnabled()) {
            logger.trace("body raw bytes \r\n{}", ByteUtils.bytesAsHexString(bytes, dumpBytes));
        }

        if (1 == header.getBasicVer()) {
            HttpHeaders headers = new DefaultHttpHeaders();
            HttpMethod method = HttpMethod.GET;
            HttpResponseStatus responseStatus = HttpResponseStatus.OK;
            String msgType = "";
            for (KVHeader kvHeader : header.getJsonHeader()) {
                if (Constants.JSON_HEADER_HTTP_METHOD.equals(kvHeader.getKey())) {
                    method = HttpMethod.valueOf(kvHeader.getValue());
                } else if (Constants.JSON_HEADER_HTTP_RESPONSE_STATUS.equals(kvHeader.getKey())) {
                    responseStatus = HttpResponseStatus.valueOf(Integer.parseInt(kvHeader.getValue()));
                } else if (Constants.JSON_HEADER_HTTP_PACKAGE_TYPE.equals(kvHeader.getKey())) {
                    msgType = kvHeader.getValue();
                } else {
                    headers.add(kvHeader.getKey(), kvHeader.getValue());
                }
            }

            if (msgType.equals(Constants.JSON_HEADER_HTTP_PACKAGE_TYPE_REQUEST)) {
                HttpRequestExt request = new HttpRequestExt(HttpVersion.HTTP_1_1, method,
                        header.getMessageCode(), Unpooled.wrappedBuffer(bytes));
                request.headers().add(headers);
                request.setIdentification(header.getTransactionAsUUID());
                out.add(request);
            } else if (msgType.equals(Constants.JSON_HEADER_HTTP_PACKAGE_TYPE_RESPONSE)) {
                headers.set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);
                HttpResponseExt bean = new HttpResponseExt(HttpVersion.HTTP_1_1, responseStatus,
                        header.getMessageCode(), Unpooled.wrappedBuffer(bytes));
                bean.headers().add(headers);
                bean.setIdentification(header.getTransactionAsUUID());
                out.add(bean);
            } else {
                logger.error("unknown msgType");
            }

        } else {
            logger.error("invalid basic ver, while header is {}", header);
            logger.error("raw body bytes is {}",
                    ByteUtils.bytesAsHexString(bytes, bodyLength));
            throw new RuntimeException("invalid basic ver {" + header.getBasicVer() + "}");
        }
    }

    public void setDumpBytes(int dumpBytes) {
        this.dumpBytes = dumpBytes;
    }

    public int getDumpBytes() {
        return dumpBytes;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }
}

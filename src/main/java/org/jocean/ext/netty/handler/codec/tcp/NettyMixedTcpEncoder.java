package org.jocean.ext.netty.handler.codec.tcp;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jocean.ext.transport.protocol.pdu.PDUBean;
import org.jocean.ext.transport.protocol.pdu.PDUHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@ChannelHandler.Sharable
public class NettyMixedTcpEncoder extends MessageToByteEncoder<PDUBean> {
    private static final Logger logger = LoggerFactory.getLogger(NettyMixedTcpEncoder.class);

    @Override
    public void encode(ChannelHandlerContext ctx, PDUBean msg, ByteBuf out) throws Exception {
        final UUID id = msg.getIdentification();
        if (logger.isDebugEnabled()) {
            logger.debug("encoding PDUBean {} to wrappedBuffer", msg);
        }
        ByteBuf body = msg.getBody();
        String messageCode = msg.getPath();
        List<KVHeader> jsonHeader = msg.getJsonHeader();
        String jsonHeaderString = JSON.toJSONString(jsonHeader);
        out.writeByte(PDUHeader.MAGIC_CODE);//magicCode
        out.writeByte(1);//basicVer
        out.writeShort(PDUHeader.MIN_XIP_HEADER_LENGTH + messageCode.length() + jsonHeaderString.length());//headerLength
        out.writeInt(body.readableBytes());//bodyLength
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());//transactionId
        out.writeShort(messageCode.length());//codeLength
        out.writeBytes(messageCode.getBytes());//code
        out.writeShort(jsonHeaderString.length());//jsonHeader
        out.writeBytes(jsonHeaderString.getBytes());
        out.writeBytes(body);//body
    }
}

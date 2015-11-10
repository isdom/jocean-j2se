package org.jocean.ext.transport.protocol.pdu;

import java.util.List;

import org.jocean.ext.netty.handler.codec.tcp.KVHeader;
import org.jocean.ext.util.MutableIdentifyable;

import io.netty.buffer.ByteBuf;

public interface PDUBean extends MutableIdentifyable {

    String getPath();

    List<KVHeader> getJsonHeader();

    ByteBuf getBody();
}

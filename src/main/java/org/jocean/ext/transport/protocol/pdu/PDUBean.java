package org.jocean.ext.transport.protocol.pdu;

import io.netty.buffer.ByteBuf;
import org.jocean.ext.netty.handler.codec.tcp.KVHeader;
import org.jocean.ext.util.MutableIdentifyable;

import java.util.List;

public interface PDUBean extends MutableIdentifyable {

    String getPath();

    List<KVHeader> getJsonHeader();

    ByteBuf getBody();
}

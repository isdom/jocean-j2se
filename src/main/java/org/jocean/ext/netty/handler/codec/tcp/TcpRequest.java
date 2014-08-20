package org.jocean.ext.netty.handler.codec.tcp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import org.jocean.ext.Constants;
import org.jocean.ext.transport.protocol.pdu.PDUBean;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JSONType(ignores = {"body", "jsonHeader", "identification", "path"})
public abstract class TcpRequest implements PDUBean {
    private UUID uuid = UUID.randomUUID();

    @Override
    public List<KVHeader> getJsonHeader() {
        List<KVHeader> headers = getExtraJsonHeader();
        headers.add(new KVHeader(org.jocean.ext.Constants.JSON_HEADER_HTTP_METHOD, HttpMethod.POST));//TCP服务之间的调用默认模拟HTTP的POST请求
        headers.add(new KVHeader(Constants.JSON_HEADER_HTTP_PACKAGE_TYPE, Constants.JSON_HEADER_HTTP_PACKAGE_TYPE_REQUEST));
        headers.add(new KVHeader(HttpHeaders.Names.CONTENT_TYPE, Constants.JSON_TYPE));
        return headers;
    }

    protected List<KVHeader> getExtraJsonHeader() {
        return new ArrayList<>();
    }

    @Override
    public ByteBuf getBody() {
        return Unpooled.wrappedBuffer(JSON.toJSONBytes(this, SerializerFeature.WriteDateUseDateFormat));
    }

    @Override
    public void setIdentification(UUID id) {
        this.uuid = id;
    }

    @Override
    public UUID getIdentification() {
        return uuid;
    }
}

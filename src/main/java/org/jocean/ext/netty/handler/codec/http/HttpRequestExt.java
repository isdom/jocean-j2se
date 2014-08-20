package org.jocean.ext.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.jocean.ext.Constants;
import org.jocean.ext.netty.handler.codec.tcp.KVHeader;
import org.jocean.ext.transport.protocol.pdu.PDUBean;
import org.jocean.idiom.Propertyable;

import java.util.*;

public class HttpRequestExt extends DefaultFullHttpRequest implements Propertyable, PDUBean {
    private Map<String, Object> properties = new HashMap<>();
    private UUID uuid = UUID.randomUUID();
    private Channel channel;

    public HttpRequestExt(HttpVersion httpVersion, HttpMethod method, String uri) {
        super(httpVersion, method, uri);
    }

    public HttpRequestExt(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content) {
        super(httpVersion, method, uri, content);
    }

    public HttpRequestExt(HttpVersion httpVersion, HttpMethod method, String uri, ByteBuf content, boolean validateHeaders) {
        super(httpVersion, method, uri, content, validateHeaders);
    }

    public static HttpRequestExt fromFullHttpRequest(final FullHttpRequest fullHttpRequest) {
        HttpRequestExt request = new HttpRequestExt(fullHttpRequest.getProtocolVersion(), fullHttpRequest.getMethod(),
                fullHttpRequest.getUri(), fullHttpRequest.content().copy());
        request.headers().add(fullHttpRequest.headers());
        return request;
    }

    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public Propertyable setProperty(String key, Object obj) {
        properties.put(key, obj);
        return this;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public void setIdentification(UUID id) {
        this.uuid = id;
    }

    @Override
    public UUID getIdentification() {
        return uuid;
    }

    @Override
    public String getPath() {
        return getUri();
    }

    @Override
    public List<KVHeader> getJsonHeader() {
        List<KVHeader> jsonHeader = new LinkedList<>();
        for (Map.Entry<String, String> entry : headers().entries()) {
            jsonHeader.add(new KVHeader(entry.getKey(), entry.getValue()));
        }
        jsonHeader.add(new KVHeader(org.jocean.ext.Constants.JSON_HEADER_HTTP_METHOD, getMethod().name()));
        jsonHeader.add(new KVHeader(Constants.JSON_HEADER_HTTP_PACKAGE_TYPE, Constants.JSON_HEADER_HTTP_PACKAGE_TYPE_REQUEST));
        return jsonHeader;
    }

    @Override
    public ByteBuf getBody() {
        return content();
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}

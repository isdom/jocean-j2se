package org.jocean.ext.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.jocean.ext.Constants;
import org.jocean.ext.netty.handler.codec.tcp.KVHeader;
import org.jocean.ext.transport.protocol.pdu.PDUBean;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HttpResponseExt extends DefaultFullHttpResponse implements PDUBean {
    private String requestUri;
    private UUID uuid;

    public HttpResponseExt(HttpVersion version, HttpResponseStatus status) {
        super(version, status);
    }

    public HttpResponseExt(HttpVersion version, HttpResponseStatus status, ByteBuf content) {
        super(version, status, content);
    }

    public HttpResponseExt(HttpVersion version, HttpResponseStatus status, String uri, ByteBuf content) {
        super(version, status, content);
        this.requestUri = uri;
    }

    public HttpResponseExt(HttpVersion version, HttpResponseStatus status, ByteBuf content, boolean validateHeaders) {
        super(version, status, content, validateHeaders);
    }

    public static HttpResponseExt fromFullHttpResponse(final FullHttpResponse fullHttpResponse) {
        HttpResponseExt bean = new HttpResponseExt(
                fullHttpResponse.getProtocolVersion(), fullHttpResponse.getStatus(), fullHttpResponse.content().copy());
        bean.headers().add(fullHttpResponse.headers());
        return bean;
    }

    @Override
    public String getPath() {
        return requestUri;
    }

    public void setPath(String requestUri) {
        this.requestUri = requestUri;
    }

    @Override
    public List<KVHeader> getJsonHeader() {
        List<KVHeader> jsonHeader = new LinkedList<>();
        for (Map.Entry<String, String> entry : headers().entries()) {
            jsonHeader.add(new KVHeader(entry.getKey(), entry.getValue()));
        }
        jsonHeader.add(new KVHeader(Constants.JSON_HEADER_HTTP_RESPONSE_STATUS, String.valueOf(getStatus().code())));
        jsonHeader.add(new KVHeader(Constants.JSON_HEADER_HTTP_PACKAGE_TYPE, Constants.JSON_HEADER_HTTP_PACKAGE_TYPE_RESPONSE));
        return jsonHeader;
    }

    @Override
    public ByteBuf getBody() {
        return content();
    }

    @Override
    public UUID getIdentification() {
        return uuid;
    }

    @Override
    public void setIdentification(UUID uuid) {
        this.uuid = uuid;
    }
}

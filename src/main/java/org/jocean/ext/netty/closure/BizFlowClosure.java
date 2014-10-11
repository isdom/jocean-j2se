package org.jocean.ext.netty.closure;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;
import org.jocean.event.api.EventReceiver;
import org.jocean.ext.flow.FlowEvents;
import org.jocean.ext.netty.handler.codec.http.HttpRequestExt;
import org.jocean.ext.netty.handler.codec.http.HttpResponseExt;
import org.jocean.ext.restful.HeaderableOutputReactor;
import org.jocean.ext.restful.PackageRegistrar;
import org.jocean.ext.transport.Sender;
import org.jocean.ext.transport.TransportUtils;
import org.jocean.ext.transport.protocol.pdu.PDUBean;
import org.jocean.ext.util.EventRegistry;
import org.jocean.idiom.Detachable;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.InterfaceSource;
import org.jocean.idiom.Pair;
import org.jocean.idiom.block.Blob;
import org.jocean.idiom.block.BlockUtils;
import org.jocean.idiom.block.PooledBytesOutputStream;
import org.jocean.idiom.pool.BytesPool;
import org.jocean.restful.OutputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class BizFlowClosure implements Closure<PDUBean> {
    private static final Logger LOG = LoggerFactory.getLogger(BizFlowClosure.class);

    @Autowired
    private PackageRegistrar registrar;
    private PooledBytesOutputStream _output;
    private EventRegistry eventRegistry = EventRegistry.getInstance();

    @Override
    public void execute(final PDUBean bean) {
        if (bean instanceof HttpResponseExt) {
            EventReceiver eventReceiver = eventRegistry.getReceiver(bean.getIdentification());
            if (eventReceiver != null) {
                try {
                    eventReceiver.acceptEvent(FlowEvents.RESPONSE_RECEIVED, bean);
                } catch (Exception e) {
                    LOG.error("", e);
                }
            } else {
                LOG.error("can't find flow of {}", bean);
            }
            return;
        }

        final HttpRequestExt msg = (HttpRequestExt) bean;
        final String contentType = msg.headers().get(HttpHeaders.Names.CONTENT_TYPE);
        byteBuf2OutputStream(msg.getBody(), this._output);
        try (final Blob blob = this._output.drainToBlob()) {
            final Pair<Object, String> flowAndEvent =
                    registrar.buildFlowMatch(msg.getMethod().name(), msg.getUri(), msg, blob, contentType, this._output);
            if (null == flowAndEvent) {
                // path not found
                writeAndFlushResponse(null, msg, null);
                return;
            }

            final InterfaceSource flow = (InterfaceSource) flowAndEvent.getFirst();
            final Detachable detachable = flow.queryInterfaceInstance(Detachable.class);

            ((OutputSource) flow).setOutputReactor(new HeaderableOutputReactor() {

                @Override
                public void output(Object representation, Map<String, String> httpHeaders) {
                    safeDetachCurrentFlow(detachable);
                    String responseJson = JSON.toJSONString(representation, SerializerFeature.PrettyFormat);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("send resp:{}", responseJson);
                    }
                    writeAndFlushResponse(responseJson, msg, httpHeaders);
                }

                @Override
                public void output(final Object representation) {
                    output(representation, null);
                }
            });

            flow.queryInterfaceInstance(EventReceiver.class)
                    .acceptEvent(flowAndEvent.getSecond());
        } catch (Exception e) {
            LOG.warn("exception when call flow's setOutputReactor, detail:{}", ExceptionUtils.exception2detail(e));
        }
    }

    private void writeAndFlushResponse(final String content, final HttpRequestExt msg, final Map<String, String> httpHeaders) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, (null != content ? OK : NO_CONTENT),
                (null != content ? Unpooled.copiedBuffer(content, CharsetUtil.UTF_8) : Unpooled.buffer(0)));

        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        if (httpHeaders != null) {
            for (Map.Entry<String, String> entry : httpHeaders.entrySet()) {
                response.headers().set(entry.getKey(), entry.getValue());
            }
        }

        Sender sender = TransportUtils.getSenderOf(msg);
        LOG.debug("SendResp {}", response);

        if (sender != null) {
            HttpResponseExt bean = HttpResponseExt.fromFullHttpResponse(response);
            bean.setPath(msg.getUri());
            bean.setIdentification(msg.getIdentification());
            sender.send(bean);
        } else {
            LOG.error("can't find sender of {}", msg);
        }
    }

    private void safeDetachCurrentFlow(final Detachable detachable) {
        if (null != detachable) {
            try {
                detachable.detach();
            } catch (Exception e) {
                LOG.warn("exception when detach current flow, detail:{}",
                        ExceptionUtils.exception2detail(e));
            }
        }
    }

    private static long byteBuf2OutputStream(final ByteBuf buf, final PooledBytesOutputStream os) {
        try (InputStream is = new ByteBufInputStream(buf)) {
            return BlockUtils.inputStream2OutputStream(is, os);
        } catch (IOException e) {
            LOG.error("", e);
        }
        return 0;
    }

    public void setBytesPool(BytesPool bytesPool) {
        this._output = new PooledBytesOutputStream(bytesPool);
    }
}

package org.jocean.ext.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.jocean.ext.transport.protocol.endpoint.Endpoint;
import org.jocean.idiom.Propertyable;

public class TransportUtils {
    private static final AttributeKey<Endpoint> TRANSPORT_ENDPOINT = AttributeKey.valueOf("TRANSPORT_ENDPOINT");
    private static final String TRANSPORT_SENDER = "TRANSPORT_SENDER";

    public static void attachEndpointToSession(ChannelHandlerContext ctx, Endpoint endpoint) {
        ctx.attr(TRANSPORT_ENDPOINT).set(endpoint);
    }

    public static Endpoint getEndpointOfSession(ChannelHandlerContext ctx) {
        return ctx.attr(TRANSPORT_ENDPOINT).get();
    }

    public static Object attachSender(Object propertyable, Sender sender) {
        if (propertyable instanceof Propertyable) {
            ((Propertyable) propertyable).setProperty(
                    TRANSPORT_SENDER, sender);
        }

        return propertyable;
    }

    public static Sender getSenderOf(Object propertyable) {
        if (propertyable instanceof Propertyable) {
            return (Sender) ((Propertyable) propertyable).getProperty(TRANSPORT_SENDER);
        }
        return null;
    }
}

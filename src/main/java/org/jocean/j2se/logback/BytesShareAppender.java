package org.jocean.j2se.logback;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;

public class BytesShareAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static List<OutputBytes> _OUTPUTS = new CopyOnWriteArrayList<>();

    public static void addOutput(final OutputBytes output) {
        _OUTPUTS.add(output);
    }

    public static void removeOutput(final OutputBytes output) {
        _OUTPUTS.remove(output);
    }

    public static void enableForRoot() {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();

        encoder.setContext(lc);
        encoder.setCharset(Charsets.UTF_8);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %5p |-%c{35}:%L - %m %n");

        final BytesShareAppender appender = new BytesShareAppender();
        appender.setName("BytesShareAppender");
        appender.setContext(lc);
        appender.setEncoder(encoder);

        encoder.start();
        appender.start();

        lc.getLogger("ROOT").addAppender(appender);
    }

    public static void disableForRoot() {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        final Appender<ILoggingEvent> appender = lc.getLogger("ROOT").getAppender("BytesShareAppender");
        if (null != appender) {
            appender.stop();
            lc.getLogger("ROOT").detachAppender(appender);
        }
    }

    @Override
    protected void append(final ILoggingEvent eventObject) {
        if (!isStarted()) {
            return;
        }

        try {
            // this step avoids LBCLASSIC-139
            if (eventObject instanceof DeferredProcessingAware) {
                ((DeferredProcessingAware) eventObject).prepareForDeferredProcessing();
            }
            // the synchronization prevents the OutputStream from being closed while we
            // are writing. It also prevents multiple threads from entering the same
            // converter. Converters assume that they are in a synchronized block.
            // lock.lock();

            final byte[] byteArray = this.encoder.encode(eventObject);
            writeBytes(byteArray);

        } catch (final IOException ioe) {
            // as soon as an exception occurs, move to non-started state
            // and add a single ErrorStatus to the SM.
            this.started = false;
            addStatus(new ErrorStatus("IO failure in appender", this, ioe));
        }
    }

    @Override
    public void start() {
        encoderInit();
        super.start();
    }

    @Override
    public void stop() {
        encoderClose();
        super.stop();
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(final Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    void encoderClose() {
        if (encoder != null) {
            try {
                final byte[] footer = encoder.footerBytes();
                writeBytes(footer);
            } catch (final IOException ioe) {
                this.started = false;
                addStatus(new ErrorStatus("Failed to write footer for appender named [" + name + "].", this, ioe));
            }
        }
    }

    void encoderInit() {
        if (encoder != null) {
            try {
                final byte[] header = encoder.headerBytes();
                writeBytes(header);
            } catch (final IOException ioe) {
                this.started = false;
                addStatus(new ErrorStatus("Failed to initialize encoder for appender named [" + name + "].", this, ioe));
            }
        }
    }

    private void writeBytes(final byte[] byteArray) throws IOException {
        if(byteArray == null || byteArray.length == 0)
            return;

        lock.lock();
        try {
            for (final Iterator<OutputBytes> iter = _OUTPUTS.iterator(); iter.hasNext(); ) {
                try {
                    iter.next().output(byteArray);
                } catch (final Exception e) {
                    // just ignore
                }
            }
        } finally {
            lock.unlock();
        }
    }

    protected Encoder<ILoggingEvent> encoder;

    /**
     * All synchronization in this class is done via the lock object.
     */
    protected final ReentrantLock lock = new ReentrantLock(false);

}

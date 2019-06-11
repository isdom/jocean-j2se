package org.jocean.j2se.cli;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Charsets;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;
import rx.functions.Action1;

public class UDSAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    static Action1<String> _OUTPUT;

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
        if (encoder != null && _OUTPUT != null) {
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
        if (encoder != null && _OUTPUT != null) {
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
            _OUTPUT.call(new String(byteArray, Charsets.UTF_8));
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

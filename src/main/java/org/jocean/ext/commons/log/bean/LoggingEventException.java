package org.jocean.ext.commons.log.bean;

public class LoggingEventException {

    private long event_id;
    private short i;
    private String trace_line;

    public long getEvent_id() {
        return event_id;
    }

    public void setEvent_id(long event_id) {
        this.event_id = event_id;
    }

    public short getI() {
        return i;
    }

    public void setI(short i) {
        this.i = i;
    }

    public String getTrace_line() {
        return trace_line;
    }

    public void setTrace_line(String trace_line) {
        this.trace_line = trace_line;
    }
}

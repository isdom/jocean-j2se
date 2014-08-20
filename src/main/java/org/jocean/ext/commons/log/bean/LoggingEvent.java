package org.jocean.ext.commons.log.bean;

import java.util.List;

public class LoggingEvent {
    private long event_id;
    private long timestmp;
    private String app_name;
    private String formatted_message;
    private String logger_name;
    private String level_string;
    private String thread_name;
    private short reference_flag;
    private String arg0;
    private String arg1;
    private String arg2;
    private String arg3;
    private String caller_filename;
    private String caller_class;
    private String caller_method;
    private int caller_line;

    private List<LoggingEventProperty> eventPropertyList;
    private List<LoggingEventException> eventExceptionList;

    public long getEvent_id() {
        return event_id;
    }

    public void setEvent_id(long event_id) {
        this.event_id = event_id;
    }

    public long getTimestmp() {
        return timestmp;
    }

    public void setTimestmp(long timestmp) {
        this.timestmp = timestmp;
    }

    public String getFormatted_message() {
        return formatted_message;
    }

    public void setFormatted_message(String formatted_message) {
        this.formatted_message = formatted_message;
    }

    public String getLogger_name() {
        return logger_name;
    }

    public void setLogger_name(String logger_name) {
        this.logger_name = logger_name;
    }

    public String getLevel_string() {
        return level_string;
    }

    public void setLevel_string(String level_string) {
        this.level_string = level_string;
    }

    public String getThread_name() {
        return thread_name;
    }

    public void setThread_name(String thread_name) {
        this.thread_name = thread_name;
    }

    public short getReference_flag() {
        return reference_flag;
    }

    public void setReference_flag(short reference_flag) {
        this.reference_flag = reference_flag;
    }

    public String getArg0() {
        return arg0;
    }

    public void setArg0(String arg0) {
        this.arg0 = arg0;
    }

    public String getArg1() {
        return arg1;
    }

    public void setArg1(String arg1) {
        this.arg1 = arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public void setArg2(String arg2) {
        this.arg2 = arg2;
    }

    public String getArg3() {
        return arg3;
    }

    public void setArg3(String arg3) {
        this.arg3 = arg3;
    }

    public String getCaller_filename() {
        return caller_filename;
    }

    public void setCaller_filename(String caller_filename) {
        this.caller_filename = caller_filename;
    }

    public String getCaller_class() {
        return caller_class;
    }

    public void setCaller_class(String caller_class) {
        this.caller_class = caller_class;
    }

    public String getCaller_method() {
        return caller_method;
    }

    public void setCaller_method(String caller_method) {
        this.caller_method = caller_method;
    }

    public int getCaller_line() {
        return caller_line;
    }

    public void setCaller_line(int caller_line) {
        this.caller_line = caller_line;
    }

    public List<LoggingEventProperty> getEventPropertyList() {
        return eventPropertyList;
    }

    public void setEventPropertyList(List<LoggingEventProperty> eventPropertyList) {
        this.eventPropertyList = eventPropertyList;
    }

    public List<LoggingEventException> getEventExceptionList() {
        return eventExceptionList;
    }

    public void setEventExceptionList(List<LoggingEventException> eventExceptionList) {
        this.eventExceptionList = eventExceptionList;
    }

    public String getApp_name() {
        return app_name;
    }

    public void setApp_name(String app_name) {
        this.app_name = app_name;
    }
}

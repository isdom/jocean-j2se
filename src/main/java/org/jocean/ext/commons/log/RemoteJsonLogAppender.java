package org.jocean.ext.commons.log;

import ch.qos.logback.classic.db.DBHelper;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.CoreConstants;
import org.jocean.ext.commons.log.bean.LogbackDbLog;
import org.jocean.ext.commons.log.bean.LoggingEvent;
import org.jocean.ext.commons.log.bean.LoggingEventException;
import org.jocean.ext.commons.log.bean.LoggingEventProperty;
import org.jocean.ext.transport.Sender;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteJsonLogAppender extends AppenderBase<ILoggingEvent> {
    static final StackTraceElement EMPTY_CALLER_DATA = CallerData.naInstance();

    private Sender messageSender;
    @Value("${app.name}")
    private String appName;
    private long lastErrorTime = System.currentTimeMillis();
    private AtomicInteger errorCounts = new AtomicInteger();

    @Override
    protected void append(ILoggingEvent eventObject) {
        //每分钟限制错误日志不超过600条,否则以system的err输出,避免和mktlog的交互异常造成死循环
        long curTime = System.currentTimeMillis();
        if (curTime - lastErrorTime > 60 * 1000) {
            lastErrorTime = curTime;
            errorCounts.set(0);
        }
        if (errorCounts.get() > 60 * 10) {
            System.err.println("========== too many errors! ===========");
            return;
        }
        errorCounts.incrementAndGet();

        if (appName == null) {
            appName = System.getProperty("app.name");
        }

        LoggingEvent event = new LoggingEvent();
        event.setApp_name(appName);
        event.setTimestmp(eventObject.getTimeStamp());
        event.setFormatted_message(eventObject.getFormattedMessage());
        event.setLogger_name(eventObject.getLoggerName());
        event.setLevel_string(eventObject.getLevel().toString());
        event.setThread_name(eventObject.getThreadName());
        event.setReference_flag(DBHelper.computeReferenceMask(eventObject));

        Object[] argArray = eventObject.getArgumentArray();
        int arrayLen = argArray != null ? argArray.length : 0;
        switch (arrayLen) {
            case 4:
                event.setArg3(asStringTruncatedTo254(argArray[3]));
            case 3:
                event.setArg2(asStringTruncatedTo254(argArray[2]));
            case 2:
                event.setArg1(asStringTruncatedTo254(argArray[1]));
            case 1:
                event.setArg0(asStringTruncatedTo254(argArray[0]));
        }

        StackTraceElement[] callerDataArray = eventObject.getCallerData();
        StackTraceElement caller = extractFirstCaller(callerDataArray);
        event.setCaller_filename(caller.getFileName());
        event.setCaller_class(caller.getClassName());
        event.setCaller_method(caller.getMethodName());
        event.setCaller_line(caller.getLineNumber());

        List<LoggingEventProperty> eventPropertyList = new ArrayList<>();
        Map<String, String> mergedMap = mergePropertyMaps(eventObject);
        for (Map.Entry<String, String> entry : mergedMap.entrySet()) {
            LoggingEventProperty eventProperty = new LoggingEventProperty();
            eventProperty.setMapped_key(entry.getKey());
            eventProperty.setMapped_value(entry.getValue());
            eventPropertyList.add(eventProperty);
        }
        event.setEventPropertyList(eventPropertyList);

        List<LoggingEventException> eventExceptionList = new ArrayList<>();
        IThrowableProxy tp = eventObject.getThrowableProxy();
        short baseIndex = 0;
        while (tp != null) {
            baseIndex = buildExceptionList(tp, baseIndex, eventExceptionList);
            tp = tp.getCause();
        }
        event.setEventExceptionList(eventExceptionList);

        messageSender.send(new LogbackDbLog(event));
    }

    public void start() {
        if (messageSender == null) {
            addError("Miss messageSender for the appender named [" + name + "].");
            return;
        }
        super.start();
    }

    private String asStringTruncatedTo254(Object o) {
        String s = null;
        if (o != null) {
            s = o.toString();
        }

        if (s == null) {
            return null;
        }
        if (s.length() <= 254) {
            return s;
        } else {
            return s.substring(0, 254);
        }
    }

    private StackTraceElement extractFirstCaller(StackTraceElement[] callerDataArray) {
        StackTraceElement caller = EMPTY_CALLER_DATA;
        if (hasAtLeastOneNonNullElement(callerDataArray))
            caller = callerDataArray[0];
        return caller;
    }

    private boolean hasAtLeastOneNonNullElement(StackTraceElement[] callerDataArray) {
        return callerDataArray != null && callerDataArray.length > 0 && callerDataArray[0] != null;
    }

    private Map<String, String> mergePropertyMaps(ILoggingEvent event) {
        Map<String, String> mergedMap = new HashMap<>();
        // we add the context properties first, then the event properties, since
        // we consider that event-specific properties should have priority over
        // context-wide properties.
        Map<String, String> loggerContextMap = event.getLoggerContextVO()
                .getPropertyMap();
        Map<String, String> mdcMap = event.getMDCPropertyMap();
        if (loggerContextMap != null) {
            mergedMap.putAll(loggerContextMap);
        }
        if (mdcMap != null) {
            mergedMap.putAll(mdcMap);
        }

        return mergedMap;
    }

    short buildExceptionList(IThrowableProxy tp, short baseIndex, List<LoggingEventException> eventExceptionList) {
        StringBuilder buf = new StringBuilder();
        ThrowableProxyUtil.subjoinFirstLine(buf, tp);
        addEventException(eventExceptionList, buf.toString(), baseIndex++);

        int commonFrames = tp.getCommonFrames();
        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
        for (int i = 0; i < stepArray.length - commonFrames; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(CoreConstants.TAB);
            ThrowableProxyUtil.subjoinSTEP(sb, stepArray[i]);
            addEventException(eventExceptionList, sb.toString(), baseIndex++);
        }

        if (commonFrames > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(CoreConstants.TAB).append("... ").append(commonFrames).append(
                    " common frames omitted");
            addEventException(eventExceptionList, sb.toString(), baseIndex++);
        }

        return baseIndex;
    }

    void addEventException(List<LoggingEventException> eventExceptionList, String txt, short i) {
        LoggingEventException eventException = new LoggingEventException();
        eventException.setI(i);
        eventException.setTrace_line(txt);
        eventExceptionList.add(eventException);
    }

    public void setMessageSender(Sender messageSender) {
        this.messageSender = messageSender;
    }
}

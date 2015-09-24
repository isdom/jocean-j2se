package org.jocean.j2se.jmx;

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.rx.OneshotSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

public class MBeanPublisher {

    private final static ObjectName MBEANSERVER_DELEGATE;
    
    static {
        MBEANSERVER_DELEGATE = MBeanUtil.safeGetObjectName("JMImplementation:type=MBeanServerDelegate");
    }
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(MBeanPublisher.class);
    
    public MBeanPublisher() {
        this(ManagementFactory.getPlatformMBeanServer());
    }
    
    public MBeanPublisher(final MBeanServerConnection mbsc) {
        this._mbsc = mbsc;
    }
    
    public Observable<MBeanStatus> watch(final ObjectName objectName) {
        return Observable.create(genOnSubscribe(objectName)).serialize();
    }

    private OnSubscribe<MBeanStatus> genOnSubscribe(final ObjectName objectName) {
        return new OnSubscribe<MBeanStatus>() {
            @Override
            public void call(final Subscriber<? super MBeanStatus> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        final NotificationListener notificationListener = 
                                new MBeanStatusNotifyListener(objectName, subscriber);
                        _mbsc.addNotificationListener(MBEANSERVER_DELEGATE, notificationListener, null, null );
                        subscriber.add(new OneshotSubscription() {
                            @Override
                            protected void doUnsubscribe() {
                                try {
                                    _mbsc.removeNotificationListener(MBEANSERVER_DELEGATE, notificationListener);
                                    subscriber.onCompleted();
                                } catch (Exception e) {
                                    LOG.warn("exception when removeNotificationListener for {}, detail:{}",
                                            objectName, ExceptionUtils.exception2detail(e));
                                }
                            }});
                    } catch (Exception e) {
                        LOG.warn("exception when addNotificationListener for {}, detail:{}",
                                objectName, ExceptionUtils.exception2detail(e));
                        subscriber.onError(e);
                    }
                    try {
                        final Set<ObjectName> mbeans = _mbsc.queryNames(objectName, null);
                        final Iterator<ObjectName> itr = mbeans.iterator();
                        while ( itr.hasNext() ) {
                            subscriber.onNext(new MBeanStatusSupport(itr.next()) {
                                @Override
                                public boolean isRegistered() {
                                    return true;
                                }
                                @Override
                                public boolean isUnregistered() {
                                    return false;
                                }});
                        }
                    } catch (Exception e) {
                        LOG.warn("exception when queryNames for {}, detail:{}",
                                objectName, ExceptionUtils.exception2detail(e));
                    }
                }
            }
            
        };
    }
    
    private final class MBeanStatusNotifyListener implements
            NotificationListener {
        private final ObjectName _objectName;
        private final Subscriber<? super MBeanStatus> _subscriber;

        private MBeanStatusNotifyListener(final ObjectName objectName,
                final Subscriber<? super MBeanStatus> subscriber) {
            this._objectName = objectName;
            this._subscriber = subscriber;
        }

        @Override
        public void handleNotification(final Notification notification,
                final Object handback) {
            if (notification.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
                //  a new MBean added
                final MBeanServerNotification mbeanNotification = (MBeanServerNotification)notification;
                if (this._objectName.apply(mbeanNotification.getMBeanName())) {
                    this._subscriber.onNext(new MBeanStatusSupport(mbeanNotification.getMBeanName()) {
                        @Override
                        public boolean isRegistered() {
                            return true;
                        }
                        @Override
                        public boolean isUnregistered() {
                            return false;
                        }} );
                }
            }
            else if (notification.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
                //  a MBean removed
                final MBeanServerNotification mbeanNotification = (MBeanServerNotification)notification;
                if (this._objectName.apply(mbeanNotification.getMBeanName())) {
                    this._subscriber.onNext(new MBeanStatusSupport(mbeanNotification.getMBeanName()) {
                        @Override
                        public boolean isRegistered() {
                            return false;
                        }
                        @Override
                        public boolean isUnregistered() {
                            return true;
                        }} );
                }
            }
            
        }
    }

    private abstract class MBeanStatusSupport implements MBeanStatus {

        private MBeanStatusSupport(final ObjectName mbeanName) {
            this._mbeanName = mbeanName;
        }

        @Override
        public ObjectName mbeanName() {
            return this._mbeanName;
        }

        @Override
        public Object getValue(final String attributeOrMethodName) {
            try {
                return _mbsc.getAttribute(this._mbeanName, attributeOrMethodName);
            } catch (AttributeNotFoundException e) {
                try {
                    return _mbsc.invoke(this._mbeanName, attributeOrMethodName, null, null);
                } catch (Exception e1) {
                    LOG.warn("exception when invoke {}.{}, detail:{}",
                            this._mbeanName, attributeOrMethodName, ExceptionUtils.exception2detail(e1));
                }
            } catch (Exception e) {
                LOG.warn("exception when get attribute {}.{}, detail:{}",
                        this._mbeanName, attributeOrMethodName, ExceptionUtils.exception2detail(e));
            }
            return null;
        }
        
        private final ObjectName _mbeanName;
    }

    private final MBeanServerConnection _mbsc;
}

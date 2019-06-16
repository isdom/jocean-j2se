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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

public class MBeanPublisher {

    private final static ObjectName MBEANSERVER_DELEGATE;

    static {
        MBEANSERVER_DELEGATE = MBeanUtil.safeGetObjectName("JMImplementation:type=MBeanServerDelegate");
    }

    private static final Logger LOG = LoggerFactory.getLogger(MBeanPublisher.class);

    public MBeanPublisher() {
        this(ManagementFactory.getPlatformMBeanServer());
    }

    public MBeanPublisher(final MBeanServerConnection mbsc) {
        this._mbsc = mbsc;
    }

    public Observable<MBeanStatus> watch(final ObjectName objectName) {
        return Observable.unsafeCreate(genOnSubscribe(objectName, null)).serialize();
    }

    public Observable<MBeanStatus> watch(final ObjectName objectName, final String notificationType) {
        // TODO
        return Observable.unsafeCreate(genOnSubscribe(objectName, notificationType)).serialize();
    }

    private OnSubscribe<MBeanStatus> genOnSubscribe(final ObjectName objectName, final String notificationType) {
        return new OnSubscribe<MBeanStatus>() {
            @Override
            public void call(final Subscriber<? super MBeanStatus> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    try {
                        final NotificationListener notificationListener =
                                new MBeanStatusNotifyListener(objectName, notificationType, subscriber);
                        _mbsc.addNotificationListener(MBEANSERVER_DELEGATE, notificationListener, null, null );
                        subscriber.add(Subscriptions.create(() -> {
                                try {
                                    _mbsc.removeNotificationListener(MBEANSERVER_DELEGATE, notificationListener);
                                    subscriber.onCompleted();
                                } catch (final Exception e) {
                                    LOG.warn("exception when removeNotificationListener for {}, detail:{}",
                                            objectName, ExceptionUtils.exception2detail(e));
                                }
                            }));
                    } catch (final Exception e) {
                        LOG.warn("exception when addNotificationListener for {}, detail:{}",
                                objectName, ExceptionUtils.exception2detail(e));
                        subscriber.onError(e);
                    }
                    try {
                        final Set<ObjectName> mbeans = _mbsc.queryNames(objectName, null);
                        final Iterator<ObjectName> itr = mbeans.iterator();
                        while (itr.hasNext()) {
                            subscriber.onNext(new MBeanStatusSupport(itr.next()) {
                                @Override
                                public int status() {
                                    return MS_REGISTERED;
                                }});
                            addNotificationListener(objectName, notificationType, subscriber);
                        }
                    } catch (final Exception e) {
                        LOG.warn("exception when queryNames for {}, detail:{}",
                                objectName, ExceptionUtils.exception2detail(e));
                    }
                }
            }

        };
    }

    static class NotificationContext {
        ObjectName objectName;
        String notificationType;
        Subscriber<? super MBeanStatus> subscriber;

        NotificationContext(final ObjectName objectName, final String notificationType, final Subscriber<? super MBeanStatus> subscriber) {
            this.objectName = objectName;
            this.notificationType = notificationType;
            this.subscriber = subscriber;
        }
    }

    private final NotificationListener _mbeanChangedListener = new NotificationListener() {
        @Override
        public void handleNotification(final Notification notification, final Object handback) {
            final NotificationContext ctx = (NotificationContext)handback;
            if (notification.getType().startsWith(ctx.notificationType)) {
                if (!ctx.subscriber.isUnsubscribed()) {
                    ctx.subscriber.onNext(new MBeanStatusSupport(ctx.objectName) {
                        @Override
                        public int status() {
                            return MS_CHANGED;
                        }});
                }
            }
        }};

    private final class MBeanStatusNotifyListener implements NotificationListener {
        private final ObjectName _objectName;
        private final Subscriber<? super MBeanStatus> _subscriber;
        private final String _notificationType;

        private MBeanStatusNotifyListener(final ObjectName objectName, final String notificationType, final Subscriber<? super MBeanStatus> subscriber) {
            this._objectName = objectName;
            this._notificationType = notificationType;
            this._subscriber = subscriber;
        }

        @Override
        public void handleNotification(final Notification notification, final Object handback) {
            if (notification.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
                //  a new MBean added
                if ( !_subscriber.isUnsubscribed()) {
                    final MBeanServerNotification mbeanNotification = (MBeanServerNotification)notification;
                    final ObjectName objectName = mbeanNotification.getMBeanName();
                    if (this._objectName.apply(objectName)) {
                        this._subscriber.onNext(new MBeanStatusSupport(objectName) {
                            @Override
                            public int status() {
                                return MS_REGISTERED;
                            }} );
                        addNotificationListener(objectName, this._notificationType, this._subscriber);
                    }
                }
            }
            else if (notification.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
                //  a MBean removed
                if ( !_subscriber.isUnsubscribed()) {
                    final MBeanServerNotification mbeanNotification = (MBeanServerNotification)notification;
                    final ObjectName objectName = mbeanNotification.getMBeanName();
                    if (this._objectName.apply(objectName)) {
                        removeNotificationListener(objectName, this._notificationType);
                        this._subscriber.onNext(new MBeanStatusSupport(mbeanNotification.getMBeanName()) {
                            @Override
                            public int status() {
                                return MS_UNREGISTERED;
                            }} );
                    }
                }
            }

        }
    }

    private void addNotificationListener(final ObjectName objectName, final String notificationType,
            final Subscriber<? super MBeanStatus> subscriber ) {
        if (null != notificationType) {
            try {
                _mbsc.addNotificationListener(objectName, _mbeanChangedListener, null,
                        new NotificationContext(objectName, notificationType, subscriber) );
                subscriber.add(Subscriptions.create(() -> removeNotificationListener(objectName, notificationType)));
            } catch (final Exception e) {
                LOG.warn("exception when addNotificationListener for {}, detail:{}", objectName, ExceptionUtils.exception2detail(e));
            }
        }
    }

    private void removeNotificationListener(final ObjectName objectName, final String notificationType) {
        if (null != notificationType) {
            try {
                _mbsc.removeNotificationListener(objectName, _mbeanChangedListener);
            } catch (final Exception e) {
                LOG.warn("exception when removeNotificationListener for {}, detail:{}", objectName, ExceptionUtils.exception2detail(e));
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
            } catch (final AttributeNotFoundException e) {
                try {
                    return _mbsc.invoke(this._mbeanName, attributeOrMethodName, null, null);
                } catch (final Exception e1) {
                    LOG.warn("exception when invoke {}.{}, detail:{}",
                            this._mbeanName, attributeOrMethodName, ExceptionUtils.exception2detail(e1));
                }
            } catch (final Exception e) {
                LOG.warn("exception when get attribute {}.{}, detail:{}",
                        this._mbeanName, attributeOrMethodName, ExceptionUtils.exception2detail(e));
            }
            return null;
        }

        private final ObjectName _mbeanName;
    }

    private final MBeanServerConnection _mbsc;
}

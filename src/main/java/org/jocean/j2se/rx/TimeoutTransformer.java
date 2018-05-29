package org.jocean.j2se.rx;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;

import rx.Observable;
import rx.Observable.Transformer;

public class TimeoutTransformer<T> implements Transformer<T, T> {

    @Override
    public Observable<T> call(final Observable<T> org) {
        return org.timeout(this._timeoutInMs, TimeUnit.MILLISECONDS);
    }

    public void setTimeout(final long timeoutInMs) {
        this._timeoutInMs = timeoutInMs;
    }

    @Value("${timeout.ms}")
    private long _timeoutInMs = 10000;
}

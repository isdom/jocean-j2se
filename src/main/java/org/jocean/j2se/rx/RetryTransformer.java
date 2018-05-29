package org.jocean.j2se.rx;

import org.jocean.idiom.rx.RxObservables;
import org.springframework.beans.factory.annotation.Value;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;

public class RetryTransformer<T> implements Transformer<T, T> {

    private Func1<? super Observable<? extends Throwable>, ? extends Observable<?>> retryPolicy() {
        return RxObservables.retryWith(errors ->
            errors.compose(RxObservables.retryIfMatch(this._exceptionType))
            .compose(RxObservables.retryMaxTimes(this._maxRetryTimes))
            .compose(RxObservables.retryDelayTo(this._retryIntervalBase)));
    }

    @Override
    public Observable<T> call(final Observable<T> org) {
        return org.retryWhen(retryPolicy());
    }

    public void setExceptionType(final Class<? extends Throwable> exceptionType) {
        this._exceptionType = exceptionType;
    }

    public void setRetryTimes(final int times) {
        this._maxRetryTimes = times;
    }

    public void setRetryIntervalBase(final int interval) {
        this._retryIntervalBase = interval;
    }

    private Class<? extends Throwable> _exceptionType;

    @Value("${retrytimes}")
    private int _maxRetryTimes = 3;

    @Value("${retryinterval}")
    private int _retryIntervalBase = 100; // 100 ms
}

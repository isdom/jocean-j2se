package org.jocean.ext.netty.closure;

/**
 * Defines a functor interface implemented by classes that do something.
 * <p/>
 * A <code>Closure</code> represents a block of code which is executed from
 * inside some block, function or iteration. It operates an input object.
 * <p/>
 */
public interface Closure<T> {

    /**
     * Performs an action on the specified input object.
     *
     * @param input the input to execute on
     */
    void execute(T input);
}

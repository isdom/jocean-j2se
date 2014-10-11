package org.jocean.ext.restful;

import org.jocean.restful.OutputReactor;

import java.util.Map;

public interface HeaderableOutputReactor extends OutputReactor {
    public void output(final Object representation, final Map<String, String> httpHeaders);
}

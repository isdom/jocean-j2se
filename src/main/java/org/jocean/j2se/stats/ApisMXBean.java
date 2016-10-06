package org.jocean.j2se.stats;

import java.util.Map;

public interface ApisMXBean {
    
    public String[] getApisAsText();

    public Map<String, Object> getApis();
}

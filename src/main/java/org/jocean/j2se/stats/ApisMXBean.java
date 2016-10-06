package org.jocean.j2se.stats;

import java.util.Map;

public interface ApisMXBean {
    
    public String[] getApisAsText();

    public Map<String, Map<String,Map<String, Integer>>> getApis();
}

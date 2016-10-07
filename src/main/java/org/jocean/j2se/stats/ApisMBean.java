package org.jocean.j2se.stats;

import java.util.Map;

public interface ApisMBean {
    
    public String[] getApisAsText();

    public Map<String, String> getApiInfo();
    
    public Map<String, Map<String,Object>> getApiStats();
}

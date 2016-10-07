package org.jocean.j2se.stats;

import java.util.Map;

public interface FlowsMBean {
    
    public Map<String, String> getFlowInfo();
    
    public Map<String, Map<String,Object>> getFlowStats();
    
    public String[] getFlowsAsText();
}

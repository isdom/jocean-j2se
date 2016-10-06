package org.jocean.j2se.stats;

import java.util.Map;

public interface FlowsMXBean {
    
    public String[] getFlowsAsText();

    public Map<String, Object> getFlows();
}

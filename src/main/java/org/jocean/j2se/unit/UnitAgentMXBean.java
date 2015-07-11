package org.jocean.j2se.unit;

import java.util.Map;

import org.springframework.context.ConfigurableApplicationContext;

public interface UnitAgentMXBean {
    
    public static interface UnitMXBean {

        public boolean isActive();
        
        public String getName();

        public String getSource();

        public String[] getParameters();

        public String[] getPlaceholders();

        public String getCreateTimestamp();
        
        public String[] getChildrenUnits();

        public String   getUnactiveReason();
        
        public void close();
    }

    public static interface SourceMXBean {

        public String[] getPlaceholders();

    }

    public boolean newUnit(
            final String unitName, 
            final String template, 
            final String[] unitParameters);

    public boolean newUnit(
            final String unitName, 
            final String template, 
            final Map<String, String> unitParameters);

    public boolean deleteUnit(final String unitName);

    public void deleteAllUnit();

    public Map<String, String[]> getSourceInfo(final String template);

    public String[] getLogs();

    public void resetLogs();
    
    public interface UnitListener {
        public void onUnitCreated(final ConfigurableApplicationContext appctx);
        public void onUnitClosed(final ConfigurableApplicationContext appctx);
    }
    
    public void addUnitListener(final UnitListener listener);
    
    public void removeUnitListener(final UnitListener listener);
}

package org.jocean.ext.restful;

import org.jocean.event.api.EventReceiverSource;
import org.jocean.event.extend.Runners;
import org.jocean.event.extend.Services;
import org.jocean.ext.util.PackageUtils;
import org.jocean.idiom.InterfaceSource;
import org.jocean.restful.RegistrarImpl;

import javax.ws.rs.Path;
import java.util.*;

public class PackageRegistrar extends RegistrarImpl {

    private String scanPackage;
    private static final EventReceiverSource sourceImpl;
    private Map<String, Object> zkNodeData = new HashMap<>();

    static {
        sourceImpl = Runners.build(new Runners.Config()
                .objectNamePrefix("public:app=" + System.getProperty("app.name", "unknown"))
                .name("flow")
                .timerService(Services.lookupOrCreateTimerService("flow"))
                .executorSource(Services.lookupOrCreateFlowBasedExecutorSource("flow")));
    }

    public PackageRegistrar() {
        super(sourceImpl);
    }

    public String getScanPackage() {
        return scanPackage;
    }

    public void setScanPackage(String scanPackage) {
        this.scanPackage = scanPackage;
        Set<Class<?>> classes = new HashSet<>();
        List<String> paths = new ArrayList<>();
        for (Class<?> clz : PackageUtils.findClassesInPackage(scanPackage, Path.class)) {
            if (InterfaceSource.class.isAssignableFrom(clz)) {
                classes.add(clz);
                paths.add(clz.getAnnotation(Path.class).value());
            }
        }
        zkNodeData.put("paths", paths);
        this.setClasses(classes);
    }

    public EventReceiverSource getSourceImpl() {
        return sourceImpl;
    }

    public Map<String, Object> getZkNodeData() {
        return this.zkNodeData;
    }
}

package org.jocean.j2se.cli.cmd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.jocean.cli.CliCommand;
import org.jocean.cli.CliContext;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.j2se.AppInfo;
import org.jocean.j2se.ModuleInfo;
import org.jocean.j2se.os.OSUtil;
import org.jocean.j2se.unit.UnitAgent;
import org.jocean.j2se.unit.UnitAgentMXBean.UnitMXBean;
import org.jocean.j2se.unit.model.ServiceConfig;
import org.jocean.j2se.unit.model.UnitDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.alibaba.edas.acm.ConfigService;
import com.alibaba.edas.acm.exception.ConfigException;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

import rx.functions.Func1;

public class StartAppCommand implements CliCommand<CliContext> {

    private static final Logger LOG = LoggerFactory.getLogger(StartAppCommand.class);

    final AtomicReference<ConfigurableApplicationContext> _ctxRef;
    final String[] _libs;

    public StartAppCommand(final AtomicReference<ConfigurableApplicationContext> ctxRef, final String[] libs) {
        this._ctxRef = ctxRef;
        this._libs = libs;
    }

    @Override
    public String execute(final CliContext ctx, final String... args) throws Exception {
        if (this._ctxRef.get() != null) {
            return "FAILED: app already started when " + new Date(this._ctxRef.get().getStartupDate());
        }

        if (args.length < 5) {
            return "FAILED: missing startapp params\n" + getHelp();
        }

        // 从控制台命名空间管理中拷贝对应值
        final Properties props = new Properties();
        props.put("endpoint", args[0]);
        props.put("namespace", args[1]);
        // 通过 ECS 实例 RAM 角色访问 ACM
        props.put("ramRoleName", args[2]);

        // 如果是加密配置，则添加下面两行进行自动解密
        //props.put("openKMSFilter", true);
        //props.put("regionId", "$regionId");

        ConfigService.init(props);

        final String defaultDataid = args[3];
        final String groupName = args[4];

        final Func1<String, String> getACMConfig = dataid -> {
            try {
                return ConfigService.getConfig(dataid, groupName, 6000);
            } catch (final ConfigException e) {
                return null;
            }
        };

        final String appName = System.getProperty("app.name", "unknown");
        final String hostname = OSUtil.getLocalHostname();

        final ServiceConfig[] confs = loadYaml(ServiceConfig[].class, appName, getACMConfig);
        final ServiceConfig conf4host = findConfig(hostname, confs);
        if (null != conf4host) {
            LOG.debug("found host-conf {} for service: {} / hostname: {}", conf4host, appName, hostname);
            buildApplication(conf4host.getConf(), getACMConfig);
            return "OK";
        }

        // try match defult config
        final ServiceConfig defaultconf = findConfig("_default_", confs);
        if (null != defaultconf) {
            LOG.debug("found default-conf {} for service: {} / hostname: {}", defaultconf, appName, hostname);
            buildApplication(defaultconf.getConf(), getACMConfig);
            return "OK";
        } else {
            LOG.debug("can't found any host-conf for service: {} match hostname: {} or default", appName, hostname);
        }

        final UnitDescription unitdesc = loadYaml(UnitDescription.class, defaultDataid, getACMConfig);
        if (null != unitdesc) {
            LOG.debug("{}/{} apply default config: {}", hostname, appName, unitdesc);
            buildApplication(new UnitDescription[]{unitdesc}, getACMConfig);
            return "OK";
        }

        LOG.debug("{}/{} NOT apply ANY config", hostname, appName);

        return "ERROR";
    }

    @SuppressWarnings("unchecked")
    private static <T> T loadYaml(final Class<T> type, final String dataid, final Func1<String, String> getACMConfig) {
        final String content = getACMConfig.call(dataid);
        if (null != content) {
            final Yaml yaml = new Yaml(new Constructor(type));

            try (final InputStream is = new ByteArrayInputStream(content.getBytes(Charsets.UTF_8))) {
                return (T)yaml.load(is);
            } catch (final IOException e) {
                LOG.warn("exception when loadYaml from {}, detail: {}", dataid, ExceptionUtils.exception2detail(e));
            }
        }
        return null;
    }

    private ServiceConfig findConfig(final String hostname, final ServiceConfig[] confs) {
        if (null != confs) {
            for (final ServiceConfig conf : confs) {
                if (conf.getHost().equals(hostname)) {
                    return conf;
                }
            }
        }
        return null;
    }

    private void buildApplication(final UnitDescription[] unitdescs, final Func1<String, String> getConfig) {
        final AbstractApplicationContext appctx = new ClassPathXmlApplicationContext("unit/clibooter.xml");
        final AppInfo app = appctx.getBean("appinfo", AppInfo.class);
        if (null != app) {
            for (final String lib : this._libs) {
                app.getModules().put(lib, new ModuleInfo(lib));
            }
        }

        if (null != unitdescs) {
            for (final UnitDescription desc : unitdescs) {
                build(appctx.getBean(UnitAgent.class), desc, null, getConfig);
            }
        }
        this._ctxRef.set(appctx);
    }

    private static final String SPRING_XML_KEY = "__spring.xml";
    private static final String REF_KEY = "->";

    private void build(final UnitAgent unitAgent, final UnitDescription desc, final String parentPath,
            final Func1<String, String> getConfig) {
        final String pathName = pathOf(parentPath, desc);
        if (desc.getName().startsWith(REF_KEY)) {
            final UnitDescription ref = buildRef(desc.getName().substring(REF_KEY.length()), unitAgent, parentPath, getConfig);
            if (null != ref) {
                buildChildren(desc.getChildren(), unitAgent, getConfig, pathOf(parentPath, ref));
            } else {
                LOG.warn("can't build unit by ref: {}, skip all it's children", desc.getName());
            }
        } else {
            final String template = getTemplateFromName(desc.getName());
            final Properties properties = desc.parametersAsProperties();
            final String[] source = genSourceFrom(properties);

            UnitMXBean unit = null;
            if (null != source ) {
                unit = unitAgent.createUnitWithSource(
                        pathName,
                        source,
                        Maps.fromProperties(properties));
            } else {
                unit = unitAgent.createUnit(
                        pathName,
                        new String[]{"**/"+ template + ".xml", template + ".xml"},
                        Maps.fromProperties(properties),
                        true);
            }
            if (null == unit) {
                LOG.info("create unit {} failed.", pathName);
            } else {
                LOG.info("create unit {} success with active status:{}", pathName, unit.isActive());
            }
            buildChildren(desc.getChildren(), unitAgent, getConfig, pathName);
        }
    }

    private String pathOf(final String parentPath, final UnitDescription desc) {
        return null != parentPath ? parentPath + desc.getName() : desc.getName();
    }

    private void buildChildren(final UnitDescription[] children,
            final UnitAgent unitAgent,
            final Func1<String, String> getConfig, final String pathName) {
        for ( final UnitDescription child : children) {
            build(unitAgent, child, pathName + "/", getConfig);
        }
    }

    private UnitDescription buildRef(final String dataid, final UnitAgent unitAgent, final String parentPath,
            final Func1<String, String> getConfig) {
        final String content = getConfig.call(dataid);
        if (null != content) {
            final Yaml yaml = new Yaml(new Constructor(UnitDescription.class));

            try (final InputStream is = new ByteArrayInputStream(content.getBytes(Charsets.UTF_8))) {
                final UnitDescription refdesc = (UnitDescription)yaml.load(is);
                if (null != refdesc) {
                    LOG.info("try to build unit by ref: {}", refdesc.getName());
                    build(unitAgent, refdesc, parentPath, getConfig);
                    return  refdesc;
                }
            } catch (final IOException e) {
            }
        }
        return null;
    }

    private static String[] genSourceFrom(final Properties properties) {
        final String value = properties.getProperty(SPRING_XML_KEY);
        properties.remove(SPRING_XML_KEY);
        return null!=value ? value.split(",") : null;
    }

    private String getTemplateFromName(final String name) {
      // for return value, eg:
      //  and gateway.1.2.3.4 --> gateway
      final int idx = name.indexOf('.');
      return ( idx > -1 ) ? name.substring(0, idx) : name;
    }

    @Override
    public String getAction() {
        return "startapp";
    }

    @Override
    public String getHelp() {
        return "start app with zk config\r\n\tUsage: startapp [endpoint] [namespace] [ramRoleName] [dataid] [group]";
    }
}


package org.jocean.j2se.cli.cmd;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.jocean.cli.CliCommand;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.Pair;
import org.jocean.j2se.cli.AppCliContext;
import org.jocean.j2se.os.OSUtil;
import org.jocean.j2se.unit.InitializationMonitor;
import org.jocean.j2se.unit.UnitAgent;
import org.jocean.j2se.unit.UnitAgentMXBean.UnitMXBean;
import org.jocean.j2se.unit.model.ServiceConfig;
import org.jocean.j2se.unit.model.UnitDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.edas.acm.ConfigService;
import com.alibaba.edas.acm.exception.ConfigException;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

import rx.functions.Func2;

public class AKStartAppCommand implements CliCommand<AppCliContext> {

    private static final Logger LOG = LoggerFactory.getLogger(AKStartAppCommand.class);

    @Inject
    UnitAgent _unitAgent;

    @Inject
    InitializationMonitor _initializationMonitor;

    boolean _started = false;

    @Override
    public String execute(final AppCliContext ctx, final String... args) throws Exception {
        if (_started) {
            return "FAILED: app already started.";
        }

        _started = true;

        if (args.length < 4) {
            return "FAILED: missing akstartapp params\n" + getHelp();
        }

        ctx.enableSendbackLOG();

        return doStartApp(args);
    }

    private String doStartApp(final String... args) {
        LOG.info("AKStartAppCommand: doStartApp with {} {} {} {}", args[0], args[1], args[2], args[3]);
        
        // 从控制台命名空间管理中拷贝对应值
        final Properties props = new Properties();
        props.put("endpoint", args[0]);
        props.put("namespace", args[1]);
        final String[] ak = args[2].split("@");
        // 通过 accessKey@secretKey 访问 ACM
        props.put("accessKey", ak[0]);
        props.put("secretKey", ak[1]);

        LOG.info("{}-{}", ak[0], ak[1]);

        // 如果是加密配置，则添加下面两行进行自动解密
        //props.put("openKMSFilter", true);
        //props.put("regionId", "$regionId");

        ConfigService.init(props);

        final String[] ss = args[3].split("@");

        final String service2conf = ss[0];
        final String defaultGroup = ss.length > 1 ? ss[1] : "DEFAULT_GROUP";

        final Func2<String, String, String> getACMConfig = (dataid, group) -> {
            try {
                return ConfigService.getConfig(dataid, null != group ? group : defaultGroup, 6000);
            } catch (final ConfigException e) {
                return null;
            }
        };

        final String appname = System.getProperty("app.name", "unknown");
        final String hostname = OSUtil.getLocalHostname();

        final Map<String, String> configs = asStringStringMap(null, (Map<Object, Object>)loadYaml(Map.class, service2conf, getACMConfig));
        if (null != configs) {
            // appName.hostName
            final String dataidAndGroup = getDataidAndGroup(appname, hostname, configs);
            if (null != dataidAndGroup) {
                return loadConfigAndBuildUnits(dataidAndGroup, hostname, getACMConfig);
            } else {
                LOG.warn("not found any config for appname:{}/hostname:{}", appname, hostname);
            }
        } else {
            LOG.warn("not found configs matched {}@{}", service2conf, defaultGroup);
        }

        return "ERROR";
    }

    private String loadConfigAndBuildUnits(final String dataidAndGroup, final String hostname, final Func2<String, String, String> getACMConfig) {
        final ServiceConfig[] confs = loadYaml(ServiceConfig[].class, dataidAndGroup, getACMConfig);
        final ServiceConfig conf4host = findConfig(hostname, confs);
        if (null != conf4host) {
            LOG.info("found host-conf {} for hostname: {}", conf4host, hostname);
            buildApplication(conf4host.getConf(), getACMConfig);
            return "OK";
        }

        // try match defult config
        final ServiceConfig defaultconf = findConfig("_default_", confs);
        if (null != defaultconf) {
            LOG.info("found default-conf {} for hostname: {}", defaultconf, hostname);
            buildApplication(defaultconf.getConf(), getACMConfig);
            return "OK";
        } else {
            LOG.warn("can't found any host-conf match hostname: {} or default", hostname);
        }

        return "ERROR";
    }

    private String getDataidAndGroup(final String appname, final String hostname, final Map<String, String> configs) {
        final String dataid_group4app_host = configs.get(appname + "." + hostname);
        if (null != dataid_group4app_host) {
            LOG.info("found config {} for appname:{}/hostname:{}", dataid_group4app_host, appname, hostname);
            return dataid_group4app_host;
        }
        final String dataid_group4app = configs.get(appname + "._default_");
        if (null != dataid_group4app) {
            LOG.info("found config {} for appname:{} with all host", dataid_group4app, appname);
            return dataid_group4app;
        }
        final String dataid_group4default = configs.get("_default_");
        if (null != dataid_group4default) {
            LOG.info("using config {} as default", dataid_group4default);
            return dataid_group4default;
        }
        return null;
    }

    private static <T> T loadYaml(final Class<T> type, final String dataidAndGroup, final Func2<String, String, String> getACMConfig) {
        final String[] ss = dataidAndGroup.split("@");

        final String dataid = ss[0];
        final String group = ss.length > 1 ? ss[1] : null;
        final String content = getACMConfig.call(dataid, group);
        if (null != content) {
            try (final InputStream is = new ByteArrayInputStream(content.getBytes(Charsets.UTF_8))) {
                return (T)new Yaml().loadAs(is, type);
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

    private void buildApplication(final UnitDescription[] unitdescs, final Func2<String, String, String> getConfig) {
        if (null != unitdescs) {
            for (final UnitDescription desc : unitdescs) {
                final long begin = System.currentTimeMillis();
                build(this._unitAgent, desc, null, null, null, getConfig);
                try {
                    LOG.info("wait for {} initialization", desc);
                    this._initializationMonitor.await();
                    LOG.info("{} has been initialized, cost {} seconds", desc, (System.currentTimeMillis() - begin) / 1000.0f);
                } catch (final InterruptedException e) {
                    LOG.warn("Interrupted when await for initialization, detail: {}", ExceptionUtils.exception2detail(e));
                }
            }
        }
    }

    private static final String SPRING_XML_KEY = "__spring.xml";
    private static final String REF_KEY = "->";

    private void build(final UnitAgent unitAgent,
            final UnitDescription desc,
            final String aliasName,
            final Map<String, String> externProps,
            final String orgParentPath,
            final Func2<String, String, String> getConfig) {
        final Pair<String, String> nameAndParentPath = initNameAndPath(orgParentPath, desc.getName(), unitAgent);
        if (null == nameAndParentPath) {
            return;
        }
        final String unitName = nameAndParentPath.first;
        final String parentPath = nameAndParentPath.second;
        final String pathName = pathOf(parentPath, aliasOr(aliasName, unitName));
        final Map<String, String> props = data2props(desc.parametersAsByteArray(), externProps);
        if (unitName.contains(REF_KEY)) {
            final String[] ss = unitName.split(REF_KEY);
            final String alias = aliasOr(aliasName, ss[0]);
            final UnitDescription ref = buildRef(ss[1], alias, props, unitAgent, parentPath, getConfig);
            if (null != ref) {
                buildChildren(desc.getChildren(), unitAgent, getConfig, pathOf(parentPath, !alias.isEmpty() ? alias : ref.getName()));
            } else {
                LOG.warn("can't build unit by ref: {}, skip all it's children", unitName);
            }
        } else {
            final String template = getTemplateFromName(unitName);
            final String[] source = genSourceFrom(props);

            UnitMXBean unit = null;
            if (null != source ) {
                unit = unitAgent.createUnitWithSource(pathName, source, props);
            } else {
                unit = unitAgent.createUnit(pathName, template2source(template), props, true);
            }
            if (null == unit) {
                LOG.info("create unit {} failed.", pathName);
            } else {
                LOG.info("create unit {} success with active status:{}", pathName, unit.isActive());
            }
            buildChildren(desc.getChildren(), unitAgent, getConfig, pathName);
        }
    }

    private static String aliasOr(final String alias, final String org) {
        return (null != alias && !alias.isEmpty()) ? alias : org;
    }

    private Pair<String, String> initNameAndPath(final String orgParentPath, final String rawname, final UnitAgent unitAgent) {
        final String ss[] = rawname.split("/");
        if ( ss.length == 1) {
            return Pair.of(rawname, orgParentPath);
        } else if ( ss.length == 2) {
            // like <parent>/<name>
            final String parentPath = unitAgent.findUnitFullpathByName(ss[0]);
            if (null != parentPath) {
                return Pair.of(ss[1], parentPath + "/");
            } else {
                LOG.warn("can't find parent Unit named {}, cancel creating child unit {}", ss[0], ss[1]);
                return null;
            }
        } else {
            LOG.warn("rawname {} has '/' > 1, cancel create this unit", rawname);
            return null;
        }
    }

    private String[] template2source(final String template) {
        return new String[]{"**/"+ template + ".xml", template + ".xml"};
    }

    private Map<String, String> data2props(final byte[] data, final Map<String, String> externProps)  {
        if (null == data) {
            if (null == externProps) {
                return Collections.emptyMap();
            } else {
                return externProps;
            }
        }
        try {
            final Map<String, String> props = loadYamlOrProperties(data);
            if (null != externProps) {
                props.putAll(externProps);
            }
            LOG.debug("parse result: {}", props);
            return props;
        } catch (final Exception e) {
            LOG.warn("exception when data2props, detail: {}", ExceptionUtils.exception2detail(e));
            if (null == externProps) {
                return Collections.emptyMap();
            } else {
                return externProps;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> loadYamlOrProperties(final byte[] data) throws IOException {
        final DataInput di = ByteStreams.newDataInput(data);
        final String _1_line = di.readLine();
        if (null != _1_line && _1_line.startsWith("## yaml")) {
            LOG.debug("parse as yaml");
            // read as yaml
            return asStringStringMap(null, (Map<Object, Object>)new Yaml().loadAs(new ByteArrayInputStream(data), Map.class));
        } else {
            return Maps.newHashMap(Maps.fromProperties(loadProperties(data)));
        }
    }

    private Map<String, String> asStringStringMap(final String prefix, final Map<Object, Object> map) {
        if (null == map) {
            return null;
        }
        final Map<String, String> ssmap = Maps.newHashMap();
        for(final Map.Entry<Object, Object> entry : map.entrySet()) {
            final String key = withPrefix(prefix, entry.getKey().toString());
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<Object, Object> orgmap = (Map<Object, Object>)entry.getValue();
                ssmap.putAll(asStringStringMap(key, orgmap));
            } else {
                ssmap.put(key, entry.getValue().toString());
            }
        }
        return ssmap;
    }

    private String withPrefix(final String prefix, final String key) {
        return null != prefix ? prefix + "." + key : key;
    }

    private Properties loadProperties(final byte[] data) {
        try (final InputStream is = null != data ? new ByteArrayInputStream(data) : null) {
            return new Properties() {
                private static final long serialVersionUID = 1L;
            {
                if (null != is) {
                    this.load( is );
                }
            }};
        } catch (final IOException e) {
            LOG.warn("exception when loadProperties, detail: {}", ExceptionUtils.exception2detail(e));
            return new Properties();
        }
    }

    private String pathOf(final String parentPath, final String unitName) {
        return null != parentPath ? parentPath + unitName : unitName;
    }

    private void buildChildren(final UnitDescription[] children,
            final UnitAgent unitAgent,
            final Func2<String, String, String> getConfig,
            final String pathName) {
        for ( final UnitDescription child : children) {
            build(unitAgent, child, null, null, pathName + "/", getConfig);
        }
    }

    private UnitDescription buildRef(
            final String dataidAndGroup,
            final String aliasName,
            final Map<String, String> props,
            final UnitAgent unitAgent,
            final String parentPath,
            final Func2<String, String, String> getConfig) {
        final String[] ss = dataidAndGroup.split("@");
        final String dataid = ss[0];
        final String group = ss.length > 1 ? ss[1] : null;
        LOG.info("try to load ref unit with dataid:{}/group:{}", dataid, null != group ? group : "(using default)");
        final String content = getConfig.call(dataid, group);
        if (null != content) {
            try (final InputStream is = new ByteArrayInputStream(content.getBytes(Charsets.UTF_8))) {
                final UnitDescription refdesc = (UnitDescription)new Yaml().loadAs(is, UnitDescription.class);
                if (null != refdesc) {
                    LOG.info("try to build unit by ref named:{} with group:{}", refdesc.getName(), null != group ? group : "(using default)");
                    build(unitAgent, refdesc, aliasName, props, parentPath, getConfig);
                    return  refdesc;
                }
            } catch (final IOException e) {
            }
        } else {
            LOG.warn("can't load config from dataid:{}/group:{}", dataid, null != group ? group : "(using default)");
        }
        return null;
    }

    private static String[] genSourceFrom(final Map<String, String> props) {
        final String value = props.get(SPRING_XML_KEY);
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
        return "akstartapp";
    }

    @Override
    public String getHelp() {
        return "start app with zk config with accessKey & secretKey\r\n\tUsage: akstartapp [endpoint] [namespace] [accessKey@secretKey] [service<-->conf's dataid]@[group]";
    }
}


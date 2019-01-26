package org.jocean.j2se.cli.cmd;

import java.io.StringReader;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.jocean.cli.CliCommand;
import org.jocean.cli.CliContext;
import org.jocean.j2se.AppInfo;
import org.jocean.j2se.ModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.edas.acm.ConfigService;

public class StartAppCommand implements CliCommand<CliContext> {

    @SuppressWarnings("unused")
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

        if (args.length < 6) {
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

        // 主动获取配置
        final String content = ConfigService.getConfig(args[3], args[4], 6000);
        final Properties properties = new Properties();
        properties.load(new StringReader(content));

        final PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();

        configurer.setProperties(properties);

        // "unit/zkbooter.xml"
        final AbstractApplicationContext appctx = new ClassPathXmlApplicationContext(new String[]{args[5]}, false);
        appctx.addBeanFactoryPostProcessor(configurer);
        appctx.refresh();

        this._ctxRef.set(appctx);

        final AppInfo app = appctx.getBean("appinfo", AppInfo.class);
        if (null != app) {
            for (final String lib : this._libs) {
                app.getModules().put(lib, new ModuleInfo(lib));
            }
        }

        return "OK";
    }

    @Override
    public String getAction() {
        return "startapp";
    }

    @Override
    public String getHelp() {
        return "start app with zk config\r\n\tUsage: startapp [endpoint] [namespace] [ramRoleName] [dataid] [group] [rootxml]";
    }
}


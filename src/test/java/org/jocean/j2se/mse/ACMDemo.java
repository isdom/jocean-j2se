package org.jocean.j2se.mse;

import java.util.Properties;

import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.edas.acm.ConfigService;
import com.alibaba.edas.acm.exception.ConfigException;

public class ACMDemo {

    private static final Logger LOG = LoggerFactory.getLogger(ACMDemo.class);

    public static void main(final String[] args) {
        LOG.info("ConfigService.init");
        
        // 从控制台命名空间管理中拷贝对应值
        final Properties props = new Properties();
        props.put("endpoint", args[0]);
        props.put("namespace", args[1]);
        // 通过 ECS 实例 RAM 角色访问 ACM
        props.put("ramRoleName", args[2]);

        ConfigService.init(props);

        try {
            LOG.info("before ConfigService.getConfig");
            final String strval = ConfigService.getConfig(args[3], args[4], 6000);
            LOG.info("ConfigService.getConfig dataId/group --> {}/{}: value --> {}", args[3], args[4], strval);
            LOG.info("after ConfigService.getConfig");
        } catch (final ConfigException e) {
        	LOG.warn("exception: {}", ExceptionUtils.exception2detail(e));
        }
    }

}

package org.jocean.ext.commons.db;

import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.PoolUtil;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.Map;

/**
 * 统计执行超时的SQL语句，使用父类的方法
 */
public class ConnectionHookImpl extends AbstractConnectionHook {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHookImpl.class);

    @Override
    public void onQueryExecuteTimeLimitExceeded(ConnectionHandle handle, Statement statement, String sql, Map<Object, Object> logParams, long timeElapsedInNs) {
        StringBuilder sb = new StringBuilder("Query execute time ").append(Math.round(timeElapsedInNs/1000000)).append("ms limit exceeded. Query: ");
        sb.append(PoolUtil.fillLogParams(sql, logParams));
        logger.warn(sb.toString());
    }
}

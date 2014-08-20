package org.jocean.ext.util;

import java.util.UUID;

/**
 * 定义具有唯一标识号接口
 */
public interface Identifyable {

    /**
     * 获取标识号
     *
     * @return 唯一标记号
     */
    public UUID getIdentification();
}

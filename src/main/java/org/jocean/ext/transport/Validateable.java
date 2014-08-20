package org.jocean.ext.transport;

/**
 * 实现该接口的bean,解码后都会调用validate方法进行验证
 */
public interface Validateable {

    /**
     * 校验请求参数是否合法,不合法的请求时，返回对应的应答给对方
     * @return
     *      校验通过时返回null，否则返回要发送给对方的对象
     */
    public Object validate();
}

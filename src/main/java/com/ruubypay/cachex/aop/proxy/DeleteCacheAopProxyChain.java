package com.ruubypay.cachex.aop.proxy;

import java.lang.reflect.Method;

/**
 * 删除缓存的注解拦截层
 * @author chenhaiyang
 */
public interface DeleteCacheAopProxyChain {

    /**
     * 
     * 获取参数
     * @return 参数
     */
    Object[] getArgs();

    
    /**
     * 获取目标实例
     * @return 实例
     */
    Object getTarget();

    /**
     * 
     * 获取方法
     * @return 方法
     */
    Method getMethod();

}

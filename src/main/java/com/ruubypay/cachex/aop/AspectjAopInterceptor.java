package com.ruubypay.cachex.aop;

import com.ruubypay.cachex.CacheHandler;
import com.ruubypay.cachex.annotation.CacheDelete;
import com.ruubypay.cachex.aop.proxy.aspectj.AspectjCacheProxyChain;
import com.ruubypay.cachex.aop.proxy.aspectj.AspectjDeleteCacheAopProxyChain;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import com.ruubypay.cachex.annotation.CacheEnabled;

import java.util.Objects;

/**
 * 使用aspectj 实现AOP拦截
 * 注意，要拦截的类的要拦截的方法不能有重名方法
 * @author chenhaiyang
 */
public class AspectjAopInterceptor {
    /**
     * 处理缓存逻辑的核心实现类
     */
    private final CacheHandler cacheHandler;

    public AspectjAopInterceptor(CacheHandler cacheHandler) {
        this.cacheHandler = Objects.requireNonNull(cacheHandler);
    }

    /**
     * 处理查询缓存的方法
     * @param aopProxyChain 切点
     * @param cacheEnabled 拦截到的注解
     * @return 返回执行结果
     * @throws Throwable 抛出异常
     */
    public Object proceed(ProceedingJoinPoint aopProxyChain,CacheEnabled cacheEnabled) throws Throwable {
        return cacheHandler.proceed(new AspectjCacheProxyChain(aopProxyChain),cacheEnabled);
    }

    /**
     * 被拦截的方法执行后 删除缓存
     * @param aopProxyChain 切点
     * @param cacheDelete 缓存删除注解
     * @param returnValue 拦截方法的返回值
     * @throws Throwable 异常
     */
    public void deleteCacheAfter(JoinPoint aopProxyChain, CacheDelete cacheDelete,Object returnValue) throws Throwable {
        cacheHandler.deleteCache(new AspectjDeleteCacheAopProxyChain(aopProxyChain), cacheDelete,false);
    }

    /**
     * 被拦截的方法执行前 删除缓存
     * @param aopProxyChain 切点
     * @param cacheDelete 缓存删除注解
     * @throws Throwable 异常
     */
    public void deleteCacheBefore(JoinPoint aopProxyChain, CacheDelete cacheDelete) throws Throwable {
        cacheHandler.deleteCache(new AspectjDeleteCacheAopProxyChain(aopProxyChain), cacheDelete,true);
    }

}

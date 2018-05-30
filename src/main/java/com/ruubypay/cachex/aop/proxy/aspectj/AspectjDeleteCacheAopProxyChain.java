package com.ruubypay.cachex.aop.proxy.aspectj;

import com.ruubypay.cachex.aop.proxy.DeleteCacheAopProxyChain;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 删除缓存的注解拦截层
 * @author chenhaiyang
 */
public class AspectjDeleteCacheAopProxyChain implements DeleteCacheAopProxyChain {

    private JoinPoint jp;

    public AspectjDeleteCacheAopProxyChain(JoinPoint jp) {
        this.jp=jp;
    }

    @Override
    public Object[] getArgs() {
        return jp.getArgs();
    }

    @Override
    public Object getTarget() {
        return jp.getTarget();
    }

    @Override
    public Method getMethod() {
        Signature signature=jp.getSignature();
        MethodSignature methodSignature=(MethodSignature)signature;
        return methodSignature.getMethod();
    }

}

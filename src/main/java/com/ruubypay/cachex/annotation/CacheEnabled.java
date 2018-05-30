package com.ruubypay.cachex.annotation;

import java.lang.annotation.*;

/**
 * 限流的每个key
 * @author chenhaiyang
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface CacheEnabled {

    /**
     * 缓存的key (可以是一个表达式)
     * @return 缓存的key
     */
    String key();

    /**
     * 缓存的时间：单位 秒,配置为0代表缓存永远不过期
     * @return 返回缓存的时间
     */
    int expire() default 0;

    /**
     * 是否为本key开启缓存,默认是开启的
     * @return 缓存开关
     */
    boolean useCache() default true;

    /**
     * 配置是否来源于外部，默认是关闭的
     * 配置可以是在注解上写死的，也可以注入一个key-value的map。
     * key缓存key,value为配置useCache,cacheTime。例如：true,1000 也可以设置为true,0 或者false
     * @return 是否通过map的形式注入key
     */
    boolean configFromOuter() default false;

}

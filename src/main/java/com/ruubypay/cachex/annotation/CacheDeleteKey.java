package com.ruubypay.cachex.annotation;

import java.lang.annotation.*;

/**
 * 删除缓存的key
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface CacheDeleteKey{
    /**
     * 删除缓存的key名称
     * @return key名称，可以是一个spel表达式
     */
    String key();
}

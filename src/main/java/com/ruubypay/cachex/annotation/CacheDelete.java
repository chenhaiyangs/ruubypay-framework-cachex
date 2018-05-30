package com.ruubypay.cachex.annotation;

import java.lang.annotation.*;

/**
 * 删除缓存的注解
 * 假设，用户系统有根据id查询，根据手机号查询，根据姓名查询
 * 则缓存可能有 getuserbyid+${id},getuserbyname+${name},getuserbyphone+${phone}
 * 那么，为了保证数据的一致性。任意的写请求（增删改）和该用户相关的所有key，则需要全部移出缓存。
 * @author chenhaiyang
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface CacheDelete {
    /**
     * 要删除的每一个缓存的Key
     * @return 返回key名称
     */
    CacheDeleteKey[] value();
}
